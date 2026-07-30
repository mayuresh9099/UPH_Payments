package com.company.workflow.exception;

public class ActionExecutionException extends WorkflowException {

    public ActionExecutionException(String message) {
        super(message);
    }

    public ActionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
