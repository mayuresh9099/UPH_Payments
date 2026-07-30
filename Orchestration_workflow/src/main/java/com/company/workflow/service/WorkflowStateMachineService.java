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

@Service
@RequiredArgsConstructor
public class WorkflowStateMachineService {

    public static final String WORKFLOW_INSTANCE_ID_HEADER = "workflowInstanceId";

    private final StateMachineFactory<WorkflowState, WorkflowEvent> stateMachineFactory;
    private final WorkflowStateMachineListener listener;

    public StateMachine<WorkflowState, WorkflowEvent> start(Long workflowInstanceId) {
        StateMachine<WorkflowState, WorkflowEvent> stateMachine =
                stateMachineFactory.getStateMachine("workflow-" + workflowInstanceId);
        stateMachine.getExtendedState().getVariables().put(WORKFLOW_INSTANCE_ID_HEADER, workflowInstanceId);
        stateMachine.startReactively().block();
        listener.persistCurrentState(workflowInstanceId, stateMachine);
        return stateMachine;
    }

    public WorkflowState sendEvent(
            StateMachine<WorkflowState, WorkflowEvent> stateMachine,
            Long workflowInstanceId,
            WorkflowEvent event
    ) {
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

    public void stop(StateMachine<WorkflowState, WorkflowEvent> stateMachine) {
        stateMachine.stopReactively().block();
    }
}
