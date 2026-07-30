package com.company.workflow.service;

import com.company.workflow.exception.WorkflowException;
import com.company.workflow.listener.WorkflowStateMachineListener;
import com.company.workflow.state.WorkflowEvent;
import com.company.workflow.state.WorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Manages the lifecycle of a Spring State Machine per workflow instance.
 * <p>
 * Each workflow instance gets its own isolated state machine identified by
 * {@code "workflow-{workflowInstanceId}"}. Events are sent reactively and
 * every state transition is persisted via {@link WorkflowStateMachineListener}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WorkflowStateMachineService {

    /** Header key used to correlate state machine events with the database workflow instance. */
    public static final String WORKFLOW_INSTANCE_ID_HEADER = "workflowInstanceId";

    private final StateMachineFactory<WorkflowState, WorkflowEvent> stateMachineFactory;
    private final WorkflowStateMachineListener listener;

    /**
     * Creates and starts a new state machine for the given workflow instance.
     *
     * @param workflowInstanceId the database ID of the workflow instance
     * @return the started state machine
     */
    public StateMachine<WorkflowState, WorkflowEvent> start(Long workflowInstanceId) {
        StateMachine<WorkflowState, WorkflowEvent> stateMachine =
                stateMachineFactory.getStateMachine("workflow-" + workflowInstanceId);
        stateMachine.getExtendedState().getVariables()
                .put(WORKFLOW_INSTANCE_ID_HEADER, workflowInstanceId);
        stateMachine.startReactively().block();
        listener.persistCurrentState(workflowInstanceId, stateMachine);
        return stateMachine;
    }

    /**
     * Sends a {@link WorkflowEvent} to the state machine and persists the resulting state.
     *
     * @param stateMachine       the active state machine
     * @param workflowInstanceId the database ID of the workflow instance
     * @param event              the event to fire
     * @return the new {@link WorkflowState} after the transition
     * @throws WorkflowException if the event is rejected
     */
    public WorkflowState sendEvent(
            StateMachine<WorkflowState, WorkflowEvent> stateMachine,
            Long workflowInstanceId,
            WorkflowEvent event) {

        Message<WorkflowEvent> message = MessageBuilder.withPayload(event)
                .setHeader(WORKFLOW_INSTANCE_ID_HEADER, workflowInstanceId)
                .build();
        Boolean accepted = stateMachine.sendEvent(Mono.just(message))
                .any(result -> result.getResultType().name().equals("ACCEPTED"))
                .block();
        if (!Boolean.TRUE.equals(accepted)) {
            throw new WorkflowException("State machine rejected event " + event
                    + " from state " + stateMachine.getState().getId());
        }
        listener.persistCurrentState(workflowInstanceId, stateMachine);
        return stateMachine.getState().getId();
    }

    /**
     * Stops and releases the state machine resources.
     *
     * @param stateMachine the state machine to stop
     */
    public void stop(StateMachine<WorkflowState, WorkflowEvent> stateMachine) {
        stateMachine.stopReactively().block();
    }
}
