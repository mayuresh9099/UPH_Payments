package com.company.workflow.factory;

import com.company.workflow.action.WorkflowAction;
import com.company.workflow.exception.WorkflowException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Central factory for resolving {@link WorkflowAction} implementations by name.
 * <p>
 * All Spring-managed {@link WorkflowAction} beans are auto-registered at startup.
 * To add a new action, implement {@link WorkflowAction}, annotate with {@code @Component},
 * and insert its name in the {@code payment_workflow_action} table — no other code changes
 * are required.
 * </p>
 */
@Component
public class ActionFactory {

    private final Map<String, WorkflowAction> actionMap;

    /**
     * Constructor injection: Spring supplies all {@link WorkflowAction} beans.
     *
     * @param actions all registered workflow action implementations
     */
    public ActionFactory(List<WorkflowAction> actions) {
        this.actionMap = actions.stream()
                .collect(Collectors.toUnmodifiableMap(WorkflowAction::getActionName, Function.identity()));
    }

    /**
     * Resolves a {@link WorkflowAction} by its action name.
     *
     * @param actionName the action name (matches {@link WorkflowAction#getActionName()})
     * @return the resolved action
     * @throws WorkflowException if no action is registered for the given name
     */
    public WorkflowAction getAction(String actionName) {
        WorkflowAction action = actionMap.get(actionName);
        if (action == null) {
            throw new WorkflowException("No WorkflowAction registered for name: " + actionName);
        }
        return action;
    }
}
