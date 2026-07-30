package com.company.workflow.util;

import com.company.workflow.exception.WorkflowException;
import com.company.workflow.state.WorkflowEvent;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps action names to their corresponding state machine success and failure events.
 * <p>
 * This mapping must be updated whenever a new action is added to the payment workflow.
 * </p>
 */
@Component
public class WorkflowEventResolver {

    private static final Map<String, WorkflowEvent> SUCCESS_EVENTS = Map.of(
            "VALIDATE_PAYMENT", WorkflowEvent.VALIDATE_PAYMENT_SUCCESS,
            "FIRCO_SCREENING", WorkflowEvent.FIRCO_SCREENING_SUCCESS,
            "FLEX_POSTING", WorkflowEvent.FLEX_POSTING_SUCCESS,
            "ACCOUNTING", WorkflowEvent.ACCOUNTING_SUCCESS,
            "NOTIFICATION", WorkflowEvent.NOTIFICATION_SUCCESS
    );

    private static final Map<String, WorkflowEvent> FAILURE_EVENTS = Map.of(
            "VALIDATE_PAYMENT", WorkflowEvent.VALIDATE_PAYMENT_FAILED,
            "FIRCO_SCREENING", WorkflowEvent.FIRCO_SCREENING_FAILED,
            "FLEX_POSTING", WorkflowEvent.FLEX_POSTING_FAILED,
            "ACCOUNTING", WorkflowEvent.ACCOUNTING_FAILED,
            "NOTIFICATION", WorkflowEvent.NOTIFICATION_FAILED
    );

    /**
     * Returns the success event for the given action name.
     *
     * @param actionName the name of the workflow action
     * @return the corresponding success {@link WorkflowEvent}
     * @throws WorkflowException if no mapping exists
     */
    public WorkflowEvent successEvent(String actionName) {
        return eventFor(SUCCESS_EVENTS, actionName, "success");
    }

    /**
     * Returns the failure event for the given action name.
     *
     * @param actionName the name of the workflow action
     * @return the corresponding failure {@link WorkflowEvent}
     * @throws WorkflowException if no mapping exists
     */
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
