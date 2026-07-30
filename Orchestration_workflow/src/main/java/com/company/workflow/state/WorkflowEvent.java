package com.company.workflow.state;

/**
 * State machine events that drive payment workflow transitions.
 * Each action has a paired SUCCESS and FAILED event.
 */
public enum WorkflowEvent {
    START_PAYMENT,
    VALIDATE_PAYMENT_SUCCESS,
    VALIDATE_PAYMENT_FAILED,
    FIRCO_SCREENING_SUCCESS,
    FIRCO_SCREENING_FAILED,
    FLEX_POSTING_SUCCESS,
    FLEX_POSTING_FAILED,
    ACCOUNTING_SUCCESS,
    ACCOUNTING_FAILED,
    NOTIFICATION_SUCCESS,
    NOTIFICATION_FAILED,
    RETRY,
    FAIL
}
