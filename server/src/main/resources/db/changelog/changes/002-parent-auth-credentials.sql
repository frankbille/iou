--liquibase formatted sql

--changeset frankbille:002-parent-auth-credentials context:schema

CREATE TABLE parent_auth_credentials
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id     BIGINT       NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_parent_auth_credentials PRIMARY KEY (id),
    CONSTRAINT uq_parent_auth_credentials_email UNIQUE (email),
    CONSTRAINT fk_parent_auth_credentials_parent FOREIGN KEY (parent_id) REFERENCES parents (id) ON DELETE RESTRICT
) COMMENT='Email/password login methods for reusable parent identities. Multiple credentials may later point to the same parent.';

CREATE INDEX idx_parent_auth_credentials_parent ON parent_auth_credentials (parent_id);
