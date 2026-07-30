-- =============================================================================
-- V1: Payment Workflow Orchestration Schema (MySQL)
-- =============================================================================

-- ── Workflow definitions ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_workflow (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    active       TINYINT(1) DEFAULT 1 NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_payment_workflow_name UNIQUE (workflow_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Action definitions per workflow (sequence-driven) ─────────────────────────
CREATE TABLE IF NOT EXISTS payment_workflow_action (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id      BIGINT NOT NULL,
    action_name      VARCHAR(100) NOT NULL,
    action_sequence  INT NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_pwa_workflow FOREIGN KEY (workflow_id)
        REFERENCES payment_workflow (id) ON DELETE CASCADE,
    CONSTRAINT uq_pwa_workflow_seq UNIQUE (workflow_id, action_sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Workflow instance (one per payment execution) ─────────────────────────────
CREATE TABLE IF NOT EXISTS payment_workflow_instance (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name         VARCHAR(100) NOT NULL,
    business_key          VARCHAR(100) NOT NULL,
    current_state         VARCHAR(50)  NOT NULL,
    status                VARCHAR(50)  NOT NULL,
    workflow_retry_count  INT DEFAULT 0 NOT NULL,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Action instance audit log ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_action_instance (
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
    started_at           TIMESTAMP NULL,
    completed_at         TIMESTAMP NULL,
    CONSTRAINT fk_pai_instance FOREIGN KEY (workflow_instance_id)
        REFERENCES payment_workflow_instance (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Retry configuration (database-driven, no code change needed) ──────────────
CREATE TABLE IF NOT EXISTS retry_configuration (
    action_name             VARCHAR(100) PRIMARY KEY,
    max_retry               INT NOT NULL,
    retry_interval_seconds  INT NOT NULL,
    backoff_strategy        VARCHAR(20) DEFAULT 'FIXED' NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_pwi_business_key  ON payment_workflow_instance (business_key);
CREATE INDEX idx_pwi_status        ON payment_workflow_instance (status);
CREATE INDEX idx_pai_instance_id   ON payment_action_instance  (workflow_instance_id);
CREATE INDEX idx_pwa_workflow_id   ON payment_workflow_action   (workflow_id);

-- =============================================================================
-- Seed data – Payment workflow definition
-- =============================================================================
INSERT IGNORE INTO payment_workflow (workflow_name, description, active)
VALUES ('PAYMENT', 'Standard retail payment processing workflow', 1);

INSERT IGNORE INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'VALIDATE_PAYMENT', 1 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT IGNORE INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'FIRCO_SCREENING', 2 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT IGNORE INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'FLEX_POSTING', 3 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT IGNORE INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'ACCOUNTING', 4 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

INSERT IGNORE INTO payment_workflow_action (workflow_id, action_name, action_sequence)
SELECT id, 'NOTIFICATION', 5 FROM payment_workflow WHERE workflow_name = 'PAYMENT';

-- ── Retry configuration seed ──────────────────────────────────────────────────
INSERT IGNORE INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('VALIDATE_PAYMENT', 0, 0, 'FIXED');

INSERT IGNORE INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('FIRCO_SCREENING', 3, 2, 'EXPONENTIAL');

INSERT IGNORE INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('FLEX_POSTING', 3, 2, 'EXPONENTIAL');

INSERT IGNORE INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('ACCOUNTING', 3, 2, 'LINEAR');

INSERT IGNORE INTO retry_configuration (action_name, max_retry, retry_interval_seconds, backoff_strategy)
VALUES ('NOTIFICATION', 3, 2, 'FIXED');
