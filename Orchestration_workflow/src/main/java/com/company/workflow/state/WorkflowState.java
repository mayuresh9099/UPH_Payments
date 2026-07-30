package com.company.workflow.state;

public enum WorkflowState {
    PAYMENT_RECEIVED,
    REQUEST_VALIDATION,
    FIRCO_SCREENING,
    PAYMENT_POSTING,
    ACCOUNTING,
    NOTIFICATION,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED
}
