--liquibase formatted sql

--changeset codex:001-initial-schema

CREATE TABLE parents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_parents PRIMARY KEY (id)
) COMMENT='Reusable parent identities.';

CREATE TABLE children (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_children PRIMARY KEY (id)
) COMMENT='Reusable child identities.';

CREATE TABLE families (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    currency_code VARCHAR(32) NOT NULL,
    currency_name VARCHAR(255) NOT NULL,
    currency_symbol VARCHAR(32) NOT NULL,
    currency_position VARCHAR(32) NOT NULL,
    currency_minor_unit INT NOT NULL,
    currency_kind VARCHAR(32) NOT NULL,
    default_reward_account_id BIGINT NULL,
    recurring_task_completion_grace_period_days INT NOT NULL,
    CONSTRAINT pk_families PRIMARY KEY (id),
    CONSTRAINT chk_families_currency_position CHECK (currency_position IN ('PREFIX', 'SUFFIX')),
    CONSTRAINT chk_families_currency_kind CHECK (currency_kind IN ('ISO_CURRENCY', 'CUSTOM')),
    CONSTRAINT chk_families_currency_minor_unit CHECK (currency_minor_unit >= 0),
    CONSTRAINT chk_families_grace_period_days CHECK (recurring_task_completion_grace_period_days >= 0)
) COMMENT='Default reward account membership must stay in the same family. The application must ensure every persisted family is assigned a default reward account after the initial account row is created.';

CREATE TABLE family_parents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    relation VARCHAR(255) NOT NULL,
    CONSTRAINT pk_family_parents PRIMARY KEY (id),
    CONSTRAINT uq_family_parents_family_parent UNIQUE (family_id, parent_id),
    CONSTRAINT fk_family_parents_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_family_parents_parent FOREIGN KEY (parent_id) REFERENCES parents (id) ON DELETE RESTRICT
) COMMENT='Family-scoped parent memberships.';

CREATE INDEX idx_family_parents_parent ON family_parents (parent_id);

CREATE TABLE family_children (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    relation VARCHAR(255) NOT NULL,
    reward_payout_policy_override VARCHAR(32) NULL,
    CONSTRAINT pk_family_children PRIMARY KEY (id),
    CONSTRAINT uq_family_children_family_child UNIQUE (family_id, child_id),
    CONSTRAINT fk_family_children_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_family_children_child FOREIGN KEY (child_id) REFERENCES children (id) ON DELETE RESTRICT,
    CONSTRAINT chk_family_children_reward_payout_policy_override CHECK (reward_payout_policy_override IS NULL OR reward_payout_policy_override IN ('ON_COMPLETION', 'ON_APPROVAL'))
) COMMENT='Family-scoped child memberships.';

CREATE INDEX idx_family_children_child ON family_children (child_id);

CREATE TABLE parent_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    invited_by_parent_id BIGINT NOT NULL,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    resolved_parent_id BIGINT NULL,
    invitation_nonce VARCHAR(255) NOT NULL,
    CONSTRAINT pk_parent_invitations PRIMARY KEY (id),
    CONSTRAINT fk_parent_invitations_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_parent_invitations_resolved_parent FOREIGN KEY (resolved_parent_id) REFERENCES parents (id) ON DELETE RESTRICT,
    CONSTRAINT fk_parent_invitations_invited_by_family_parent FOREIGN KEY (family_id, invited_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT chk_parent_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
) COMMENT='Invitations remain as records after acceptance, revocation, or expiration.';

CREATE INDEX idx_parent_invitations_lookup ON parent_invitations (family_id, email, status, expires_at);
CREATE INDEX idx_parent_invitations_nonce ON parent_invitations (invitation_nonce);

CREATE TABLE money_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    CONSTRAINT pk_money_accounts PRIMARY KEY (id),
    CONSTRAINT uq_money_accounts_family_name UNIQUE (family_id, name),
    CONSTRAINT uq_money_accounts_family_id UNIQUE (family_id, id),
    CONSTRAINT fk_money_accounts_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT chk_money_accounts_kind CHECK (kind IN ('CASH', 'BANK', 'SAVINGS', 'CUSTOM'))
) COMMENT='Family-scoped money storage locations. The application must prevent deleting the configured default reward account.';

ALTER TABLE families
    ADD CONSTRAINT fk_families_default_reward_account
    FOREIGN KEY (id, default_reward_account_id) REFERENCES money_accounts (family_id, id) ON DELETE RESTRICT;

CREATE TABLE task_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_task_categories PRIMARY KEY (id),
    CONSTRAINT uq_task_categories_family_name UNIQUE (family_id, name),
    CONSTRAINT uq_task_categories_family_id UNIQUE (family_id, id),
    CONSTRAINT fk_task_categories_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE
) COMMENT='Family-scoped task categories.';

CREATE TABLE one_off_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category_id BIGINT NULL,
    reward_amount_minor INT NOT NULL,
    reward_payout_policy VARCHAR(32) NOT NULL,
    eligibility_mode VARCHAR(32) NOT NULL,
    created_by_parent_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_by_parent_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    completed_child_id BIGINT NULL,
    completed_at DATETIME(6) NULL,
    approved_at DATETIME(6) NULL,
    approved_by_parent_id BIGINT NULL,
    CONSTRAINT pk_one_off_tasks PRIMARY KEY (id),
    CONSTRAINT uq_one_off_tasks_family_id UNIQUE (family_id, id),
    CONSTRAINT fk_one_off_tasks_category FOREIGN KEY (family_id, category_id) REFERENCES task_categories (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_one_off_tasks_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_one_off_tasks_created_by_parent FOREIGN KEY (family_id, created_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT fk_one_off_tasks_updated_by_parent FOREIGN KEY (family_id, updated_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT fk_one_off_tasks_completed_child FOREIGN KEY (family_id, completed_child_id) REFERENCES family_children (family_id, child_id) ON DELETE RESTRICT,
    CONSTRAINT fk_one_off_tasks_approved_by_parent FOREIGN KEY (family_id, approved_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT chk_one_off_tasks_reward_amount_minor CHECK (reward_amount_minor > 0),
    CONSTRAINT chk_one_off_tasks_reward_payout_policy CHECK (reward_payout_policy IN ('ON_COMPLETION', 'ON_APPROVAL')),
    CONSTRAINT chk_one_off_tasks_eligibility_mode CHECK (eligibility_mode IN ('ALL_CHILDREN', 'RESTRICTED')),
    CONSTRAINT chk_one_off_tasks_status CHECK (status IN ('AVAILABLE', 'COMPLETED', 'APPROVED')),
    CONSTRAINT chk_one_off_tasks_completion_state CHECK (
        (status = 'AVAILABLE' AND completed_child_id IS NULL AND completed_at IS NULL AND approved_at IS NULL AND approved_by_parent_id IS NULL)
        OR (status = 'COMPLETED' AND completed_child_id IS NOT NULL AND completed_at IS NOT NULL AND approved_at IS NULL AND approved_by_parent_id IS NULL)
        OR (status = 'APPROVED' AND completed_child_id IS NOT NULL AND completed_at IS NOT NULL AND approved_at IS NOT NULL AND approved_by_parent_id IS NOT NULL)
    )
) COMMENT='Eligible child rows are interpreted by eligibility_mode. Same-family checks for eligible children remain application-enforced because the join table does not carry family_id.';

CREATE TABLE recurring_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category_id BIGINT NULL,
    reward_amount_minor INT NOT NULL,
    reward_payout_policy VARCHAR(32) NOT NULL,
    eligibility_mode VARCHAR(32) NOT NULL,
    created_by_parent_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_by_parent_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    recurrence_kind VARCHAR(32) NOT NULL,
    recurrence_interval INT NULL,
    recurrence_day_of_month INT NULL,
    recurrence_starts_on DATE NULL,
    recurrence_ends_on DATE NULL,
    recurrence_max_completions_per_period INT NULL,
    CONSTRAINT pk_recurring_tasks PRIMARY KEY (id),
    CONSTRAINT uq_recurring_tasks_family_id UNIQUE (family_id, id),
    CONSTRAINT fk_recurring_tasks_category FOREIGN KEY (family_id, category_id) REFERENCES task_categories (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_tasks_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_tasks_created_by_parent FOREIGN KEY (family_id, created_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_tasks_updated_by_parent FOREIGN KEY (family_id, updated_by_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT chk_recurring_tasks_reward_amount_minor CHECK (reward_amount_minor > 0),
    CONSTRAINT chk_recurring_tasks_reward_payout_policy CHECK (reward_payout_policy IN ('ON_COMPLETION', 'ON_APPROVAL')),
    CONSTRAINT chk_recurring_tasks_eligibility_mode CHECK (eligibility_mode IN ('ALL_CHILDREN', 'RESTRICTED')),
    CONSTRAINT chk_recurring_tasks_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_recurring_tasks_recurrence_kind CHECK (recurrence_kind IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM')),
    CONSTRAINT chk_recurring_tasks_recurrence_numbers CHECK (
        (recurrence_interval IS NULL OR recurrence_interval > 0)
        AND (recurrence_day_of_month IS NULL OR recurrence_day_of_month BETWEEN 1 AND 31)
        AND (recurrence_max_completions_per_period IS NULL OR recurrence_max_completions_per_period > 0)
        AND (recurrence_ends_on IS NULL OR recurrence_starts_on IS NULL OR recurrence_ends_on >= recurrence_starts_on)
    )
) COMMENT='Recurrence configuration is stored inline. Same-family checks for eligible children remain application-enforced because the join table does not carry family_id.';

CREATE TABLE recurring_task_recurrence_days (
    recurring_task_id BIGINT NOT NULL,
    day_of_week VARCHAR(32) NOT NULL,
    CONSTRAINT pk_recurring_task_recurrence_days PRIMARY KEY (recurring_task_id, day_of_week),
    CONSTRAINT fk_recurring_task_recurrence_days_task FOREIGN KEY (recurring_task_id) REFERENCES recurring_tasks (id) ON DELETE CASCADE,
    CONSTRAINT chk_recurring_task_recurrence_days_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
) COMMENT='Normalized days-of-week for recurring task definitions.';

CREATE TABLE one_off_task_eligible_children (
    one_off_task_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    CONSTRAINT pk_one_off_task_eligible_children PRIMARY KEY (one_off_task_id, child_id),
    CONSTRAINT fk_one_off_task_eligible_children_task FOREIGN KEY (one_off_task_id) REFERENCES one_off_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_one_off_task_eligible_children_child FOREIGN KEY (child_id) REFERENCES children (id) ON DELETE RESTRICT
) COMMENT='Same-family child validation remains application-enforced because family_id is not stored on this join table.';

CREATE INDEX idx_one_off_task_eligible_children_child ON one_off_task_eligible_children (child_id);

CREATE TABLE recurring_task_eligible_children (
    recurring_task_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    CONSTRAINT pk_recurring_task_eligible_children PRIMARY KEY (recurring_task_id, child_id),
    CONSTRAINT fk_recurring_task_eligible_children_task FOREIGN KEY (recurring_task_id) REFERENCES recurring_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_task_eligible_children_child FOREIGN KEY (child_id) REFERENCES children (id) ON DELETE RESTRICT
) COMMENT='Same-family child validation remains application-enforced because family_id is not stored on this join table.';

CREATE INDEX idx_recurring_task_eligible_children_child ON recurring_task_eligible_children (child_id);

CREATE TABLE recurring_task_completions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recurring_task_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    occurrence_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    completed_at DATETIME(6) NULL,
    approved_at DATETIME(6) NULL,
    approved_by_parent_id BIGINT NULL,
    CONSTRAINT pk_recurring_task_completions PRIMARY KEY (id),
    CONSTRAINT uq_recurring_task_completions_task_child_occurrence UNIQUE (recurring_task_id, child_id, occurrence_date),
    CONSTRAINT fk_recurring_task_completions_task FOREIGN KEY (recurring_task_id) REFERENCES recurring_tasks (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_task_completions_child FOREIGN KEY (child_id) REFERENCES children (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_task_completions_approved_by_parent FOREIGN KEY (approved_by_parent_id) REFERENCES parents (id) ON DELETE RESTRICT,
    CONSTRAINT chk_recurring_task_completions_status CHECK (status IN ('AVAILABLE', 'COMPLETED', 'APPROVED')),
    CONSTRAINT chk_recurring_task_completions_completion_state CHECK (
        (status = 'AVAILABLE' AND completed_at IS NULL AND approved_at IS NULL AND approved_by_parent_id IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND approved_at IS NULL AND approved_by_parent_id IS NULL)
        OR (status = 'APPROVED' AND completed_at IS NOT NULL AND approved_at IS NOT NULL AND approved_by_parent_id IS NOT NULL)
    )
) COMMENT='Same-family validation between recurring tasks and completion children remains application-enforced because the completion row does not carry family_id.';

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    timestamp DATETIME(6) NOT NULL,
    amount_minor INT NOT NULL,
    description TEXT NULL,
    owner_parent_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    to_account_id BIGINT NULL,
    from_account_id BIGINT NULL,
    account_id BIGINT NULL,
    adjustment_reason VARCHAR(32) NULL,
    one_off_task_id BIGINT NULL,
    recurring_task_completion_id BIGINT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT uq_transactions_one_off_task UNIQUE (one_off_task_id),
    CONSTRAINT uq_transactions_recurring_task_completion UNIQUE (recurring_task_completion_id),
    CONSTRAINT fk_transactions_family FOREIGN KEY (family_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_owner_parent FOREIGN KEY (family_id, owner_parent_id) REFERENCES family_parents (family_id, parent_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_child FOREIGN KEY (family_id, child_id) REFERENCES family_children (family_id, child_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_to_account FOREIGN KEY (family_id, to_account_id) REFERENCES money_accounts (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_from_account FOREIGN KEY (family_id, from_account_id) REFERENCES money_accounts (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_account FOREIGN KEY (family_id, account_id) REFERENCES money_accounts (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_one_off_task FOREIGN KEY (family_id, one_off_task_id) REFERENCES one_off_tasks (family_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_recurring_task_completion FOREIGN KEY (recurring_task_completion_id) REFERENCES recurring_task_completions (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_transaction_type CHECK (transaction_type IN ('REWARD', 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'ADJUSTMENT')),
    CONSTRAINT chk_transactions_amount_minor CHECK (amount_minor > 0),
    CONSTRAINT chk_transactions_adjustment_reason CHECK (adjustment_reason IS NULL OR adjustment_reason IN ('INITIAL_BALANCE', 'MANUAL_ADD', 'MANUAL_REMOVE')),
    CONSTRAINT chk_transactions_transfer_accounts_differ CHECK (transaction_type <> 'TRANSFER' OR from_account_id <> to_account_id),
    CONSTRAINT chk_transactions_reward_links CHECK (
        (transaction_type = 'REWARD' AND ((one_off_task_id IS NOT NULL AND recurring_task_completion_id IS NULL) OR (one_off_task_id IS NULL AND recurring_task_completion_id IS NOT NULL)))
        OR (transaction_type <> 'REWARD' AND one_off_task_id IS NULL AND recurring_task_completion_id IS NULL)
    ),
    CONSTRAINT chk_transactions_variant_columns CHECK (
        (transaction_type = 'REWARD' AND owner_parent_id IS NOT NULL AND child_id IS NOT NULL AND to_account_id IS NOT NULL AND from_account_id IS NULL AND account_id IS NULL AND adjustment_reason IS NULL)
        OR (transaction_type = 'DEPOSIT' AND owner_parent_id IS NOT NULL AND child_id IS NOT NULL AND to_account_id IS NOT NULL AND from_account_id IS NULL AND account_id IS NULL AND adjustment_reason IS NULL AND one_off_task_id IS NULL AND recurring_task_completion_id IS NULL)
        OR (transaction_type = 'WITHDRAWAL' AND owner_parent_id IS NOT NULL AND child_id IS NOT NULL AND from_account_id IS NOT NULL AND to_account_id IS NULL AND account_id IS NULL AND adjustment_reason IS NULL AND one_off_task_id IS NULL AND recurring_task_completion_id IS NULL)
        OR (transaction_type = 'TRANSFER' AND owner_parent_id IS NOT NULL AND child_id IS NOT NULL AND from_account_id IS NOT NULL AND to_account_id IS NOT NULL AND account_id IS NULL AND adjustment_reason IS NULL AND one_off_task_id IS NULL AND recurring_task_completion_id IS NULL)
        OR (transaction_type = 'ADJUSTMENT' AND owner_parent_id IS NOT NULL AND child_id IS NOT NULL AND account_id IS NOT NULL AND to_account_id IS NULL AND from_account_id IS NULL AND adjustment_reason IS NOT NULL AND one_off_task_id IS NULL AND recurring_task_completion_id IS NULL)
    )
) COMMENT='Immutable ledger entries. Same-family validation for recurring task completion links remains application-enforced because recurring_task_completions does not expose family_id directly.';

CREATE INDEX idx_transactions_family_timestamp ON transactions (family_id, timestamp);
