-- =============================================================================
-- V2: Idempotent seed data for the Payment workflow definition (MySQL).
-- INSERT IGNORE skips any row that would violate a unique/PK constraint,
-- so this script is safe to run on a database that already has the seed data.
-- =============================================================================

-- ── Workflow definition ───────────────────────────────────────────────────────
INSERT IGNORE INTO payment_workflow (workflow_name, description, active)
VALUES ('PAYMENT', 'Standard retail payment processing workflow', 1);

-- ── Action sequence ───────────────────────────────────────────────────────────
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

-- ── Retry configuration ───────────────────────────────────────────────────────
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
