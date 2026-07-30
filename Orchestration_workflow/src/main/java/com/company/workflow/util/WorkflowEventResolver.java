package com.company.workflow.util;

import com.company.workflow.exception.WorkflowException;
import com.company.workflow.state.WorkflowEvent;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkflowEventResolver {

    private static final Map<String, WorkflowEvent> SUCCESS_EVENTS = Map.of(
            "REQUEST_VALIDATION", WorkflowEvent.REQUEST_VALIDATION_SUCCESS,
            "FIRCO_SCREENING", WorkflowEvent.FIRCO_SCREENING_SUCCESS,
            "PAYMENT_POSTING", WorkflowEvent.PAYMENT_POSTING_SUCCESS,
            "ACCOUNTING", WorkflowEvent.ACCOUNTING_SUCCESS,
            "NOTIFICATION", WorkflowEvent.NOTIFICATION_SUCCESS
    );

    private static final Map<String, WorkflowEvent> FAILURE_EVENTS = Map.of(
            "REQUEST_VALIDATION", WorkflowEvent.REQUEST_VALIDATION_FAILED,
            "FIRCO_SCREENING", WorkflowEvent.FIRCO_SCREENING_FAILED,
            "PAYMENT_POSTING", WorkflowEvent.PAYMENT_POSTING_FAILED,
            "ACCOUNTING", WorkflowEvent.ACCOUNTING_FAILED,
            "NOTIFICATION", WorkflowEvent.NOTIFICATION_FAILED
    );

    public WorkflowEvent successEvent(String actionName) {
        return eventFor(SUCCESS_EVENTS, actionName, "success");
    }

    public WorkflowEvent failureEvent(String actionName) {
        return eventFor(FAILURE_EVENTS, actionName, "failure");
    }

    private WorkflowEvent eventFor(Map<String, WorkflowEvent> events, String actionName, String result) {
        WorkflowEvent event = events.get(actionName);
        if (event == null) {
            throw new WorkflowException("No " + result + " event mapped for action " + actionName);
        }
        return event;
    }
}
