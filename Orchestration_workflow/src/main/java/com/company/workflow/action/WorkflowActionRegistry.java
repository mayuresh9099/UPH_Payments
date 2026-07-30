package com.company.workflow.action;

import com.company.workflow.exception.WorkflowException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WorkflowActionRegistry {

    private final Map<String, WorkflowAction> actions;

    public WorkflowActionRegistry(List<WorkflowAction> actions) {
        this.actions = actions.stream()
                .collect(Collectors.toUnmodifiableMap(WorkflowAction::getActionName, Function.identity()));
    }

    public WorkflowAction get(String actionName) {
        WorkflowAction action = actions.get(actionName);
        if (action == null) {
            throw new WorkflowException("No WorkflowAction registered for " + actionName);
        }
        return action;
    }
}
