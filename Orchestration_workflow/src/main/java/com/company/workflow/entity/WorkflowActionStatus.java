package com.company.workflow.entity;

public enum WorkflowActionStatus {
    PENDING,
    RUNNING,
    RETRYING,
    SUCCESS,
    BUSINESS_FAILURE,
    TECHNICAL_FAILURE,
    FAILED
}
