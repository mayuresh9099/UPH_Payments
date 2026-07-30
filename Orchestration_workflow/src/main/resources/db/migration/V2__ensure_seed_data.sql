-- =============================================================================
-- V2: Idempotent seed data for the Payment workflow definition.
-- Safe to run on any database – MERGE INTO only inserts rows that are missing.
-- =============================================================================

-- ── Workflow definition ───────────────────────────────────────────────────────
MERGE INTO payment_workflow dest
USING (SELECT 'PAYMENT' AS workflow_name FROM dual) src
ON (dest.workflow_name = src.workflow_name)
WHEN NOT MATCHED THEN
    INSERT (workflow_name, description, active)
    VALUES ('PAYMENT', 'Standard retail payment processing workflow', 1);

-- ── Action sequence ───────────────────────────────────────────────────────────
MERGE INTO payment_workflow_action dest
USING (
    SELECT pw.id AS workflow_id, 'VALIDATE_PAYMENT' AS action_name, 1 AS action_sequence
    FROM payment_workflow pw WHERE pw.workflow_name = 'PAYMENT'
) src
ON (dest.workflow_id = src.workflow_id AND dest.action_sequence = src.action_sequence)
WHEN NOT MATCHED THEN
    INSERT (workflow_id, action_name, action_sequence)
    VALUES (src.workflow_id, src.action_name, src.action_sequence);

MERGE INTO payment_workflow_action dest
USING (
    SELECT pw.id AS workflow_id, 'FIRCO_SCREENING' AS action_name, 2 AS action_sequence
    FROM payment_workflow pw WHERE pw.workflow_name = 'PAYMENT'
) src
ON (dest.workflow_id = src.workflow_id AND dest.action_sequence = src.action_sequence)
WHEN NOT MATCHED THEN
    INSERT (workflow_id, action_name, action_sequence)
    VALUES (src.workflow_id, src.action_name, src.action_sequence);

MERGE INTO payment_workflow_action dest
USING (
    SELECT pw.id AS workflow_id, 'FLEX_POSTING' AS action_name, 3 AS action_sequence
    FROM payment_workflow pw WHERE pw.workflow_name = 'PAYMENT'
) src
ON (dest.workflow_id = src.workflow_id AND dest.action_sequence = src.action_sequence)
WHEN NOT MATCHED THEN
    INSERT (workflow_id, action_name, action_sequence)
    VALUES (src.workflow_id, src.action_name, src.action_sequence);

MERGE INTO payment_workflow_action dest
USING (
    SELECT pw.id AS workflow_id, 'ACCOUNTING' AS action_name, 4 AS action_sequence
    FROM payment_workflow pw WHERE pw.workflow_name = 'PAYMENT'
) src
ON (dest.workflow_id = src.workflow_id AND dest.action_sequence = src.action_sequence)
WHEN NOT MATCHED THEN
    INSERT (workflow_id, action_name, action_sequence)
    VALUES (src.workflow_id, src.action_name, src.action_sequence);

MERGE INTO payment_workflow_action dest
USING (
    SELECT pw.id AS workflow_id, 'NOTIFICATION' AS action_name, 5 AS action_sequence
    FROM payment_workflow pw WHERE pw.workflow_name = 'PAYMENT'
) src
ON (dest.workflow_id = src.workflow_id AND dest.action_sequence = src.action_sequence)
WHEN NOT MATCHED THEN
    INSERT (workflow_id, action_name, action_sequence)
    VALUES (src.workflow_id, src.action_name, src.action_sequence);

-- ── Retry configuration ───────────────────────────────────────────────────────
MERGE INTO retry_configuration dest
USING (SELECT 'VALIDATE_PAYMENT' AS action_name FROM dual) src
ON (dest.action_name = src.action_name)
WHEN NOT MATCHED THEN
    INSERT (action_name, max_retry, retry_interval_seconds, backoff_strategy)
    VALUES ('VALIDATE_PAYMENT', 0, 0, 'FIXED');

MERGE INTO retry_configuration dest
USING (SELECT 'FIRCO_SCREENING' AS action_name FROM dual) src
ON (dest.action_name = src.action_name)
WHEN NOT MATCHED THEN
    INSERT (action_name, max_retry, retry_interval_seconds, backoff_strategy)
    VALUES ('FIRCO_SCREENING', 3, 2, 'EXPONENTIAL');

MERGE INTO retry_configuration dest
USING (SELECT 'FLEX_POSTING' AS action_name FROM dual) src
ON (dest.action_name = src.action_name)
WHEN NOT MATCHED THEN
    INSERT (action_name, max_retry, retry_interval_seconds, backoff_strategy)
    VALUES ('FLEX_POSTING', 3, 2, 'EXPONENTIAL');

MERGE INTO retry_configuration dest
USING (SELECT 'ACCOUNTING' AS action_name FROM dual) src
ON (dest.action_name = src.action_name)
WHEN NOT MATCHED THEN
    INSERT (action_name, max_retry, retry_interval_seconds, backoff_strategy)
    VALUES ('ACCOUNTING', 3, 2, 'LINEAR');

MERGE INTO retry_configuration dest
USING (SELECT 'NOTIFICATION' AS action_name FROM dual) src
ON (dest.action_name = src.action_name)
WHEN NOT MATCHED THEN
    INSERT (action_name, max_retry, retry_interval_seconds, backoff_strategy)
    VALUES ('NOTIFICATION', 3, 2, 'FIXED');
