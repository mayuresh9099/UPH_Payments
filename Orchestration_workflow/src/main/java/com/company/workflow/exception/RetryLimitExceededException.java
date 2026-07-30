package com.company.workflow.exception;

public class RetryLimitExceededException extends WorkflowException {

    public RetryLimitExceededException(String message) {
        super(message);
    }
}
