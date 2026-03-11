# System specification (evolving)

## Purpose

IOU is a family-oriented system for managing children’s allowances, balances, and rewards for completing tasks. The system models money as a ledger of transactions and allows families to track where money is stored (cash, bank, etc.), how it is earned (tasks or manual actions), and how balances change over time.

The system is designed for:

* small family groups
* a single currency per family (which may represent real money or tokens)

---

## Core domain concepts

### Family

A family is the top-level container for all data.

Families are written through the following mutations:

* `createFamily`
* `updateFamily`
* `deleteFamily`

A family defines:

* the currency used
* parents and children
* task categories
* tasks
* money accounts

All balances and transactions belong to exactly one family.

All references between entities must remain within the same family boundary. For example:

* A task's `category`, if present, must belong to the same family as the task.
* `eligibleChildren` for a task must belong to the same family as the task.
* When set, a one-off task's `child` must belong to the same family as the task.
* A `RecurringTaskCompletion.child` must belong to the same family as the associated recurring task.
* Transaction references (`child`, `owner`, and accounts) must belong to the same family as the transaction.
* A `RewardTransaction.taskCompletion` must belong to the same family as the transaction.

These invariants ensure that all data relationships remain scoped to a single family.
Task categories are also family-specific. Default categories may be created when a family is set up, but they become that family's own categories and may diverge independently from categories in other families.

Within a family, task category names must be unique.

A task category cannot be deleted while tasks still reference it. Tasks must first be updated to remove or change the category.

Task categories are written through the following mutations:

* `createTaskCategory`
* `updateTaskCategory`
* `deleteTaskCategory`

Mutation semantics:

* `createTaskCategory` creates a new category within a family.
* `updateTaskCategory` renames an existing category.
* `deleteTaskCategory` deletes a category only when no tasks in the family reference it.

Task category mutation invariants:

* `createTaskCategory` and `updateTaskCategory` must enforce category name uniqueness within the family.
* `updateTaskCategory` must not move a category between families.
* `deleteTaskCategory` must fail if any one-off or recurring task still references the category.

A family intentionally models parents and children as lists rather than fixed roles. This allows the system to support many types of family structures without enforcing specific roles like "mom" or "dad".

Relationship labels are descriptive only and are stored as free text.

Family membership is written through the following mutations:

* `inviteParentToFamily`
* `revokeParentInvitation`
* `updateFamilyParent`
* `removeParentFromFamily`
* `addChildToFamily`
* `updateFamilyChild`
* `removeChildFromFamily`

Membership mutation semantics:

* `inviteParentToFamily` creates a `ParentInvitation` for an email-based parent invite into the family.
* `revokeParentInvitation` revokes a pending parent invitation.
* `updateFamilyParent` updates the relationship label for a parent's membership in the family after the parent has joined.
* `removeParentFromFamily` removes a parent's membership from the family.
* `addChildToFamily` adds a child membership to a family. This may either create a new child identity or connect an existing child identity to the family, depending on the linking flow defined later.
* `updateFamilyChild` updates child membership configuration within the family, including relation and optional reward payout override, and updates child profile data such as name where allowed.
* `removeChildFromFamily` removes a child's membership from the family.

Membership mutation invariants:

* Parents and children may each belong to multiple families.
* Membership update and removal mutations target the membership entities themselves (`FamilyParent` / `FamilyChild`) rather than only the underlying person identities, because a single person may belong to multiple families.
* Family membership is modeled through the `FamilyParent` and `FamilyChild` join types rather than exclusive ownership of a person by a single family.
* Child-level reward payout override is family-scoped behavior and therefore belongs to the child's membership in a specific family rather than to a global child identity.
* Parent membership creation is invitation-based rather than direct add-by-name.
* `ParentInvitation` is a first-class entity so families can observe invitation state before acceptance.
* Parent relationship labeling is applied to the resulting `FamilyParent` membership after acceptance rather than being stored on the invitation itself.
* A parent invitation may later resolve to an existing parent identity even if that identity is ultimately matched or confirmed through signals beyond the invited email address.
* Invitations may expire. `ParentInvitation.expiresAt` records when an invitation stops being valid, and `EXPIRED` represents an invitation that was not accepted before that time.
* Removing a child from a family must fail if any task completion or transaction in that family still references that child.
* Removing a parent from a family must fail if any task authorship, update metadata, approval metadata, or transaction ownership in that family still references that parent.
* Updating a family parent or child membership must not implicitly alter memberships in other families.
* Relation labels remain free text and have no behavioral meaning.

Identity and authentication notes:

* The authenticated `viewer.person` represents a parent or child identity that may belong to multiple families.
* Parent authentication is expected to map to a reusable parent identity, likely using email-based login.
* Parent membership addition is therefore modeled as an email invitation flow.
* Each invitation must also contain a secure nonce or invitation code used by the acceptance flow. This value is intentionally not exposed through the GraphQL schema, but it is part of the invitation domain model and is used to prove possession of the invite, typically through a link or similar acceptance mechanism.
* Invitation acceptance must be able to link to an already existing parent identity even when that identity is associated with another family through a different email alias or login address.
* The eventual transition from `ParentInvitation` into a concrete `FamilyParent` membership is intentionally left unspecified in this iteration. It may later be modeled either as a normal GraphQL mutation or as part of a security/authentication flow.
* The exact identity-proofing and invite-acceptance security flow is intentionally deferred, but the domain model must support invitation state existing independently before resolution into a concrete `FamilyParent` membership.
* Child authentication is expected to map to a reusable child identity, likely using device-based authentication initiated from a parent-managed flow.
* Family membership creation may therefore later expand from simple add operations into invitation or linking flows without changing the core multi-family person model.

A logged-in user (viewer) may belong to multiple families.

Mutation semantics:

* `createFamily` creates a new family together with its initial currency configuration, its initial default reward account, and its recurring task completion grace period.
* `updateFamily` updates family-level configuration including name, currency, configured default reward account, and recurring task completion grace period.
* `deleteFamily` permanently deletes a family and all data contained within that family boundary.

Family mutation invariants:

* `defaultRewardAccountId` must belong to the same family being updated.
* `recurringTaskCompletionGracePeriodDays` must be zero or greater.
* A value of `0` means no extra grace beyond the current eligible recurrence period.
* Updating family currency changes how money is interpreted and displayed for the family going forward. Since the currency configuration is family-owned and centralized, this is allowed by the model.
* Deleting a family removes its full contained dataset, including parents/children joins, task categories, tasks, completions, money accounts, and transactions scoped to that family.

---

### Currency

Each family has exactly one currency.

The currency is persisted as part of the family's own configuration.

The currency may represent:

* real-world money (USD, EUR, etc.)
* a custom token system (points, stars, etc.)

Money values are stored in **minor units** (integers) to avoid floating-point errors. Presentation formatting (currency symbol, placement, etc.) is handled by the client using the family's currency configuration.

`Money.amountMinor` intentionally remains an `Int` in the model unless real-world usage later demonstrates a need for larger values.

Transaction amounts are always represented as positive values. Whether a balance increases or decreases is determined by the transaction type and, for adjustments, by the adjustment reason.

Example:

5.25 → 525

---

### Money accounts

Money may be stored in multiple locations, modeled as **MoneyAccount**.

Money accounts are written through the following mutations:

* `createMoneyAccount`
* `updateMoneyAccount`
* `deleteMoneyAccount`

Examples:

* child cash drawer
* parent-held bank money
* piggy bank
* savings jar

Money accounts belong to the family and represent storage locations, not ownership.
Each money account belongs to exactly one family.
Ownership of money is determined by transactions, not by the account itself.
The same money account may hold money belonging to multiple children.

Each family must designate exactly one money account as its default reward account.
This is the account automatically used as `RewardTransaction.toAccount` when task rewards are paid out.
It represents the family's default holding location for earned money and may be conceptual rather than physically literal. For example, it can represent money the family owes a child before that money is later moved into a piggy bank, cash jar, or bank account.

Each family also defines `recurringTaskCompletionGracePeriodDays`, which controls how far back recurring task completions may be recorded after the relevant recurrence period has passed.

Every balance-affecting transaction is tied to one or more money accounts. A family that does not care about account-level detail may still use one or more default accounts internally.

A child’s total balance is derived from ledger transactions affecting that child.

Account balances are also derived from transactions.

Transactions remain the single source of truth.

Mutation semantics:

* `createMoneyAccount` creates a new money account within a family.
* `updateMoneyAccount` updates the account's configuration fields.
* `deleteMoneyAccount` permanently deletes a money account only when doing so would not violate ledger or family configuration invariants.

Money account mutation invariants:

* Money account names must be unique within a family.
* `updateMoneyAccount` must not move an account between families.
* The family's configured `defaultRewardAccount` cannot be deleted.
* A money account that is referenced by any transaction cannot be deleted.

---

### Ledger / transactions

Transactions are the **authoritative financial record** of the system.

Ledger transactions are written through the following mutations:

* `recordDeposit`
* `recordWithdrawal`
* `recordTransfer`
* `recordAdjustment`

All balances must be derivable from transactions.

Transaction types represent different financial events, including:

* rewards from completed tasks
* transfers between accounts
* deposits
* withdrawals
* manual adjustments

Each transaction records:

* the family
* the child whose balance changes
* the amount
* involved accounts
* the parent responsible for the action
* the timestamp of the event

Transactions use their own event data as their operational history. `owner` identifies the acting parent, and `timestamp` identifies when the transaction occurred. Separate created/updated metadata is not modeled.

Transaction descriptions are purely optional notes and have no additional behavioral meaning in the domain model.

The amount stored in a reward transaction represents the actual payout that occurred. Task reward values may change later, but transactions remain the source of truth for what was paid.

For task rewards, the destination account is always the family's configured default reward account. Task completion and approval do not require the client to choose a payout account.

Transactions must be immutable once created.

Balances are derived projections.

Mutation semantics:

* `recordDeposit` creates a `DepositTransaction` that adds externally sourced money into a child's account.
* `recordWithdrawal` creates a `WithdrawalTransaction` that removes money from a child's account.
* `recordTransfer` creates a `TransferTransaction` that moves a child's money between two family money accounts.
* `recordAdjustment` creates an `AdjustmentTransaction` for manual correction, starting balance setup, or exceptional balance changes.

Ledger mutation invariants:

* All referenced entities (`childId`, `fromAccountId`, `toAccountId`, `accountId`) must belong to the specified family.
* `amountMinor` must be a positive integer.
* `recordTransfer` requires `fromAccountId` and `toAccountId` to be different accounts.
* `recordAdjustment` with `INITIAL_BALANCE` represents an increase only.
* Transaction descriptions remain optional notes with no behavioral meaning.
* Once created, a transaction cannot be edited or deleted through normal domain mutations.

### Transaction balance effects

Each transaction type has a defined effect on derived balances.

* **RewardTransaction**

  * increases the child's balance
  * increases the destination account balance

* **DepositTransaction**

  * increases the child's balance
  * increases the destination account balance

* **WithdrawalTransaction**

  * decreases the child's balance
  * decreases the source account balance

* **TransferTransaction**

  * does not change the child's balance
  * decreases the source account balance
  * increases the destination account balance

* **AdjustmentTransaction**

  * changes the child's balance
  * changes the specified account balance by the same amount

Transfers only change where money is stored. They do not change how much money the child has.

Deposits, withdrawals, rewards, and adjustments change the child's total balance.

For adjustments, **INITIAL_BALANCE** always represents an increase. The system does not model negative starting balances for children.

Transaction amounts are always positive. Direction is derived from the transaction type:

* **RewardTransaction** and **DepositTransaction** increase balances
* **WithdrawalTransaction** decreases balances
* **TransferTransaction** moves money between accounts without changing the child's total balance

For **AdjustmentTransaction**, direction is determined by the adjustment reason:

* **INITIAL_BALANCE** and **MANUAL_ADD** increase balances
* **MANUAL_REMOVE** decreases balances

Additional ledger mutation invariants:

* `recordDeposit` increases both the child's derived balance and the destination account's derived balance.
* `recordWithdrawal` decreases both the child's derived balance and the source account's derived balance.
* `recordTransfer` does not change the child's derived balance.
* `recordTransfer` decreases the source account balance and increases the destination account balance.
* `recordAdjustment` changes both the child's derived balance and the specified account balance in the direction implied by `reason`.
* Shared business logic may reject transactions that would make an account or child balance invalid if the family rules require that, but the base domain model does not itself define overdraft protection.

---

### Tasks

Tasks represent work children can complete to earn rewards.

Tasks also track basic authorship and lifecycle metadata:

* who created the task
* when it was created
* who last updated it
* when it was last updated

This is intentionally lightweight operational history, not a full audit log or rollback system.

Deletion rules are intentionally conservative once a task has financial history.
A one-off task may be deleted until it has a completion that produced a reward transaction.
After that, it must remain in the system, though it may later be hidden or archived by separate view rules.
A recurring task may be deleted until its first completion exists.
After any completion exists, it may no longer be deleted and must instead be stopped or archived.

Two kinds of tasks exist:

#### One-off tasks

A one-off task is both the task definition and the completion record.

It moves through the lifecycle:

* for **ON_COMPLETION**: AVAILABLE → COMPLETED
* for **ON_APPROVAL**: AVAILABLE → COMPLETED → APPROVED

While the task is in the **AVAILABLE** state, no completing child may be recorded yet. Once the task moves beyond AVAILABLE, the completing child must be set.

Completion and approval represent the human workflow for the task. Whether money has been paid out is determined by the presence of a **RewardTransaction**, not by a separate lifecycle state.

**APPROVED** is only used when the effective payout policy requires approval before payout. For tasks using **ON_COMPLETION**, the workflow ends at **COMPLETED**.

Depending on payout policy, approval may or may not be required before the reward transaction is created.

The model intentionally avoids separate rejection or cancellation lifecycle states. A parent either moves the task back to **AVAILABLE** or deletes it.

One-off tasks are written through the following mutations:

* `createOneOffTask`
* `updateOneOffTask`
* `deleteOneOffTask`
* `completeOneOffTask`
* `approveOneOffTask`
* `resetOneOffTaskToAvailable`

Mutation semantics:

* `createOneOffTask` creates a new one-off task in **AVAILABLE** state with no child, no completion timestamp, no approval metadata, and no reward transaction.
* `updateOneOffTask` updates one-off task configuration fields only. It does not perform workflow transitions.
* `deleteOneOffTask` deletes a one-off task only while it has not produced a reward transaction.
* `completeOneOffTask` assigns the completing child, sets `completedAt`, transitions the task to **COMPLETED**, and may create a reward transaction immediately when the effective payout policy is **ON_COMPLETION**.
* `approveOneOffTask` transitions the task from **COMPLETED** to **APPROVED**, sets approval metadata, and may create a reward transaction when the effective payout policy is **ON_APPROVAL**.
* `resetOneOffTaskToAvailable` removes completion and approval state and returns the task to **AVAILABLE**.

One-off task mutation invariants:

* `createOneOffTask` and `updateOneOffTask` must enforce same-family rules for `categoryId` and `eligibleChildIds`.
* `rewardAmountMinor` must be a positive integer.
* `completeOneOffTask` is only valid when the task is currently **AVAILABLE**.
* The completing child must be eligible for the task under the current eligibility rules.
* `approveOneOffTask` is only valid when the task is currently **COMPLETED** and the effective payout policy is **ON_APPROVAL**.
* `resetOneOffTaskToAvailable` is only valid while no reward transaction exists for the task.
* `updateOneOffTask` must not change workflow fields directly.
* Once a one-off task has a reward transaction, it may no longer be deleted and may no longer be reset to **AVAILABLE**.

#### Recurring tasks

A recurring task defines work that can happen repeatedly according to a recurrence rule.

Each occurrence is represented by a **RecurringTaskCompletion**.

The recurrence configuration becomes **immutable after the first completion exists**.

To change recurrence behavior, a recurring task must be archived and replaced with a new task.
Once a recurring task has any completion, it may no longer be deleted.

The model does not currently include a separate paused state. Recurring tasks are either active or archived.
Recurring opportunities are time-bound chances to earn rewards. If a recurrence window passes without completion, that opportunity is simply lost rather than requiring explicit pause handling for vacations or other temporary interruptions.

Recurring task definitions are written through the following mutations:

* `createRecurringTask`
* `updateRecurringTask`
* `archiveRecurringTask`
* `deleteRecurringTask`

Mutation semantics:

* `createRecurringTask` creates a new recurring task with its recurrence rule in **ACTIVE** state.
* `updateRecurringTask` updates recurring task configuration fields, including recurrence, subject to recurrence immutability rules.
* `archiveRecurringTask` transitions a recurring task to **ARCHIVED**.
* `deleteRecurringTask` permanently deletes a recurring task only while it has no completions.

Recurring task mutation invariants:

* `createRecurringTask` and `updateRecurringTask` must enforce same-family rules for `categoryId` and `eligibleChildIds`.
* `rewardAmountMinor` must be a positive integer.
* `createRecurringTask` and `updateRecurringTask` must validate that the supplied recurrence input forms a valid recurrence rule.
* `updateRecurringTask` must not change workflow or completion history directly.
* Once a recurring task has any completion, `deleteRecurringTask` must fail.
* Once a recurring task has any completion, `updateRecurringTask` must not modify `recurrence`.
* `archiveRecurringTask` is the supported way to stop future use of a recurring task that already has completion history.
* `archiveRecurringTask` must not delete or alter existing completions.

Recurring task completion workflow mutations are:

* `completeRecurringTask`
* `approveRecurringTaskCompletion`
* `resetRecurringTaskCompletionToAvailable`

Recurring task completion mutation semantics:

* `completeRecurringTask` creates a concrete `RecurringTaskCompletion` for the relevant recurrence period, assigns the completing child, sets `completedAt`, transitions the completion to **COMPLETED**, and may create a reward transaction immediately when the effective payout policy is **ON_COMPLETION**.
* `approveRecurringTaskCompletion` transitions a completion from **COMPLETED** to **APPROVED**, sets approval metadata, and creates the reward transaction when the effective payout policy is **ON_APPROVAL**.
* `resetRecurringTaskCompletionToAvailable` removes completion and approval state and returns the completion to **AVAILABLE**.

Recurring task completion mutation invariants:

* `completeRecurringTask` must target an **ACTIVE** recurring task.
* The completing child must be eligible for the task under the current eligibility rules.
* When `occurrenceDate` is omitted, the system completes the current eligible recurrence period.
* When `occurrenceDate` is provided, shared business logic maps it to the intended recurrence period.
* `occurrenceDate` must not be in the future.
* `occurrenceDate` must not refer to a recurrence period outside the family's configured `recurringTaskCompletionGracePeriodDays`.
* `completeRecurringTask` must enforce `TaskRecurrence.maxCompletionsPerPeriod` for the resolved recurrence period.
* `approveRecurringTaskCompletion` is only valid when the completion is currently **COMPLETED** and the effective payout policy is **ON_APPROVAL**.
* `resetRecurringTaskCompletionToAvailable` is only valid while no reward transaction exists for the completion.
* Once a reward transaction exists for a recurring completion, it may no longer be reset to **AVAILABLE**.

---

### Task completion

Completion state is modeled through **TaskCompletion**.

Completion includes:

* which child completed the task (null while AVAILABLE)
* timestamps for completion and approval
* lifecycle state of the task workflow
* which parent approved the completion, when approval is required
* optional reward transaction representing the financial payout

The reward transaction is the authoritative indicator that a reward has been paid.
`completedAt` must be present once a completion has moved beyond **AVAILABLE**.
`approvedAt` and `approvedBy` are only used when the effective payout policy is **ON_APPROVAL** and the completion has reached **APPROVED**.

Each task completion may be linked to at most one reward transaction, and each reward transaction must belong to exactly one task completion.

The model does not separately track an actor who marked a task as completed. The `child` on the completion is the child who completed it.

Additional recurring task completion invariants:

* `RecurringTaskCompletion.child` is always non-null.
* `RecurringTaskCompletion.completedAt` must be present when status is **COMPLETED** or **APPROVED**.
* `RecurringTaskCompletion.approvedAt` and `approvedBy` must only be present when status is **APPROVED**.
* For effective payout policy **ON_COMPLETION**, `rewardTransaction` may be present once the completion reaches **COMPLETED**.
* For effective payout policy **ON_APPROVAL**, `rewardTransaction` may only be present once the completion reaches **APPROVED**.
* Completions must respect `TaskRecurrence.maxCompletionsPerPeriod`; multiple completions that exceed this limit within the same recurrence period are invalid.
* A recurring completion is created at completion time rather than being pre-generated ahead of time.
* `CompleteRecurringTaskInput.occurrenceDate`, when provided, identifies the intended recurrence period to complete.
* `occurrenceDate` must not be in the future.
* A recurring completion may only be created for the current recurrence period or for a past recurrence period that is still within the family's configured `recurringTaskCompletionGracePeriodDays`.
* Once the grace period has passed, the missed recurring opportunity is lost and cannot be completed retroactively through normal task completion flow.

The model intentionally does not include separate **REJECTED** or **CANCELED** states. If a completion is not accepted and the task should remain available, it is moved back to **AVAILABLE**. If it should no longer exist, it is deleted instead of moving to a terminal cancellation or rejection state.
A reward transaction links back to the concrete **TaskCompletion** that earned it, rather than only to the abstract task definition.

The payout policy determines whether a reward is created on completion or after approval.

* For **ON_COMPLETION**, a completion moves from **AVAILABLE** to **COMPLETED**, and the reward transaction may be created at completion time.
* For **ON_APPROVAL**, a completion moves from **AVAILABLE** to **COMPLETED** to **APPROVED**, and the reward transaction may be created when the task is approved.

**APPROVED** is only a valid lifecycle state when the effective payout policy is **ON_APPROVAL**.

---

### Reward payout

Each task defines a default **RewardPayoutPolicy**:

* ON_COMPLETION
* ON_APPROVAL

Children may optionally override the policy.

This override is intentionally modeled at the **child level** to allow families to apply different levels of autonomy or supervision depending on the child. For example, older children may operate independently with automatic payouts on completion, while younger children may require parental approval before rewards are granted.

Effective payout policy resolution (evaluated per completion):

child override → task policy

The reward amount on the transaction represents the **actual paid amount**.

When a reward transaction is created, it is always written to the family's configured default reward account rather than a client-selected account.

The task reward is only the intended reward.

A reward transaction references the concrete **TaskCompletion** that earned the reward. For recurring tasks, the recurring task definition can be reached through the linked **RecurringTaskCompletion**.

This relationship is one-to-one: a single completion can produce at most one reward transaction, and every reward transaction must reference the completion that earned it.

---

### Task eligibility

Tasks can optionally restrict which children may complete them.

Eligibility rules:

* `eligibleChildren = null` → all children in the family may complete the task
* `eligibleChildren = []` → no children are currently allowed to complete the task
* `eligibleChildren = [childA, childB]` → only those specific children may complete the task

The data model intentionally allows an empty list even though the UI will normally avoid creating that state.

---

### Recurrence

Recurring tasks define how often work becomes available.

The recurrence model is intentionally simpler than a full calendar system.

Supported patterns include:

* daily
* weekly
* monthly
* custom intervals

Field usage depends on `TaskRecurrence.kind`:

* **DAILY**

  * uses `interval` optionally
  * does not use `daysOfWeek` or `dayOfMonth`

* **WEEKLY**

  * uses `daysOfWeek`
  * may also use `interval` for every-N-weeks behavior
  * does not use `dayOfMonth`

* **MONTHLY**

  * uses `dayOfMonth`
  * may also use `interval` for every-N-months behavior
  * does not use `daysOfWeek`

* **CUSTOM**

  * uses `interval`
  * may use either `daysOfWeek` or `dayOfMonth` if the custom rule is based on a weekly-like or monthly-like pattern
  * the exact interpretation is defined by shared business logic and must still form a valid single recurrence rule

`startsOn` and `endsOn` limit the active date range of the recurrence regardless of kind.
`maxCompletionsPerPeriod` limits how many completions may be accepted within a single recurrence period.

The model does not allow mixing unrelated recurrence dimensions in one rule. For example, a rule should not use both `daysOfWeek` and `dayOfMonth` unless the chosen kind and shared business logic explicitly support that combination.

Recurrence rules become immutable once completions exist.

---

## Technical architecture

### Data loading model

The client uses a **front-loaded data model**.

When a family is opened, the client loads the majority of the family's state in a single query, including:

* family metadata
* parents and children
* tasks
* task completions
* money accounts
* transactions

After this initial load, most view transitions do **not** require additional server queries.

Recurring tasks expose their full completion history through the main family data model. This may grow over time, but the expected scale for a family system keeps this acceptable for now. More incremental synchronization approaches may be added later if needed.

Balances and projections can be calculated locally using shared domain logic.

---

### Client/server shared logic

Business rules are implemented in shared Kotlin modules so both the backend and the client compute the same results.

Examples include:

* balance derivation from transactions
* recurrence calculations
* task completion rules

---

### Backend technology

The backend is implemented using:

* Spring Boot
* Spring GraphQL
* Spring Web
* Spring Data JPA
* MySQL as the database

The system primarily exposes a GraphQL API.

For date and time values, the GraphQL schema uses human-readable date/time scalars rather than raw integer timestamps. The implementation is expected to use GraphQL Java extended scalars together with Spring GraphQL integration.

The model uses:

* `Date` for calendar-oriented values such as recurrence boundaries (`startsOn`, `endsOn`)
* `DateTime` for event moments such as task creation, updates, completion, approval, and transaction timestamps

---

### Realtime updates

The system relies heavily on **GraphQL subscriptions** to keep clients synchronized.

Subscriptions are implemented using **WebSockets**.

Instead of many fine-grained domain subscriptions, the system exposes a single **family-level subscription**:

* `familyEvents(familyId: ID!): FamilyEvent!`

Clients subscribe once for a specific family and keep that subscription active behind the application's family-scoped state/cache layer rather than only within individual views.

When changes occur within that family, the server publishes a `FamilyEvent` so the client can update its cached family state and let views react automatically.

The `FamilyEvent` stream is intentionally modeled as a unified event union rather than many separate subscriptions. This keeps the client synchronization model simple and fits the front-loaded data model.

Subscription event design rules:

* Events are primarily **snapshot-style domain events** that return the updated entity directly.
* Delete-style events return IDs rather than full entities.
* Events are family-scoped and must never expose cross-family changes.
* The unified stream is intended for cache synchronization rather than as a low-level audit/change-log protocol.
* A single user may subscribe to multiple family event streams if they belong to multiple families.

The event union currently covers:

* family updates and deletion
* parent invitation changes
* family parent membership changes/removals
* family child membership changes/removals
* money account changes/deletions
* task category changes/deletions
* one-off task changes/deletions
* recurring task changes/deletions
* recurring task completion changes
* newly recorded transactions

This ensures all clients remain consistent even though view transitions do not reload data from the server.
