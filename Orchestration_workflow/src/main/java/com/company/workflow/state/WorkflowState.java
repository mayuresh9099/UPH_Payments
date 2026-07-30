package com.company.workflow.state;

/**
 * Defines all possible states in the payment workflow lifecycle.
 *
 * <pre>
 * PAYMENT_RECEIVED
 *     → VALIDATE_PAYMENT
 *     → FIRCO_SCREENING
 *     → FLEX_POSTING
 *     → ACCOUNTING
 *     → NOTIFICATION
 *     → PAYMENT_COMPLETED
 *
 * Any failure path → PAYMENT_FAILED
 * </pre>
 */
public enum WorkflowState {
    PAYMENT_RECEIVED,
    VALIDATE_PAYMENT,
    FIRCO_SCREENING,
    FLEX_POSTING,
    ACCOUNTING,
    NOTIFICATION,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED
}
