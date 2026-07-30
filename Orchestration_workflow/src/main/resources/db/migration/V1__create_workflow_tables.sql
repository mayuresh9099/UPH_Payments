-- =============================================================================
-- V1: Payment Workflow Orchestration Schema (MySQL)
-- =============================================================================

-- ── Workflow definitions ──────────────────────────────────────────────────────
CREATE TABLE payment_workflow (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    active        TINYINT(1)  DEFAULT 1  NOT NULL,
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_payment_workflow_name UNIQUE (workflow_name)
);

-- ── Action definitions per workflow (sequence-driven) ─────────────────────────
CREATE TABLE payment_workflow_action (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id      BIGINT NOT NULL,
    action_name      VARCHAR(100) NOT NULL,
    action_sequence  INT NOT NULL,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_pwa_workflow FOREIGN KEY (workflow_id)
        REFERENCES payment_workflow (id),
    CONSTRAINT uq_pwa_workflow_seq UNIQUE (workflow_id, action_sequence)
);

-- ── Workflow instance (one per payment execution) ─────────────────────────────
CREATE TABLE payment_workflow_instance (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name         VARCHAR(100) NOT NULL,
    business_key          VARCHAR(100) NOT NULL,
    current_state         VARCHAR(50)  NOT NULL,
    status                VARCHAR(50)  NOT NULL,
    workflow_retry_count  INT DEFAULT 0 NOT NULL,
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── Action instance audit log ─────────────────────────────────────────────────
CREATE TABLE payment_action_instance (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    payment_id           VARCHAR(100) NOT NULL,
    action_name          VARCHAR(100) NOT NULL,
    state                VARCHAR(100) NOT NULL,
    action_order         INT NOT NULL,
    status               VARCHAR(50)  NOT NULL,
    retry_count          INT DEFAULT 0 NOT NULL,
    error_code           VARCHAR(100),
    error_message        LONGTEXT,
    started_at           DATETIME,
    completed_at         DATETIME,
    CONSTRAINT fk_pai_instance FOREIGN KEY (workflow_instance_id)
        REFERENCES payment_workflow_instance (id)
);

-- ── Retry configuration (database-driven, no code change needed) ──────────────
CREATE TABLE retry_configuration (
    action_name             VARCHAR(100) PRIMARY KEY,
    max_retry               INT NOT NULL,
    retry_interval_seconds  INT NOT NULL,
    backoff_strategy        VARCHAR(20)  DEFAULT 'FIXED' NOT NULL
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_pwi_business_key  ON payment_workflow_instance (business_key);
CREATE INDEX idx_pwi_status        ON payment_workflow_instance (status);
CREATE INDEX idx_pai_instance_id   ON payment_action_instance  (workflow_instance_id);
CREATE INDEX idx_pwa_workflow_id   ON payment_workflow_action   (workflow_id);

-- =============================================================================
-- Seed data – Payment workflow definition
-- =============================================================================
INSERT INTO payment_workflow (workflow_name, description, active)
VALUES ('PAYMENT', 'Standard retail payment processing workflow', 1);

INSERT INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'VALIDATE_PAYMENT', 1 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'FIRCO_SCREENING', 2 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'FLEX_POSTING', 3 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'ACCOUNTING', 4 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'NOTIFICATION', 5 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

-- ── Retry configuration seed ──────────────────────────────────────────────────
INSERT INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('VALIDATE_PAYMENT', 0, 0, 'FIXED');

INSERT INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('FIRCO_SCREENING', 3, 2, 'EXPONENTIAL');

INSERT INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('FLEX_POSTING', 3, 2, 'EXPONENTIAL');

INSERT INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('ACCOUNTING', 3, 2, 'LINEAR');

INSERT INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('NOTIFICATION', 3, 2, 'FIXED');
