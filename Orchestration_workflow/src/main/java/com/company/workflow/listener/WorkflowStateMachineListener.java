package com.company.workflow.listener;

import com.company.workflow.entity.WorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.repository.WorkflowInstanceRepository;
import com.company.workflow.state.WorkflowEvent;
import com.company.workflow.state.WorkflowState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowStateMachineListener extends StateMachineListenerAdapter<WorkflowState, WorkflowEvent> {

    private final WorkflowInstanceRepository workflowInstanceRepository;

    @Override
    public void stateEntered(State<WorkflowState, WorkflowEvent> state) {
        log.info("state={} event=state_entered", state.getId());
    }

    @Override
    public void stateExited(State<WorkflowState, WorkflowEvent> state) {
        log.info("state={} event=state_exited", state.getId());
    }

    @Override
    public void transition(Transition<WorkflowState, WorkflowEvent> transition) {
        WorkflowState source = transition.getSource() == null ? null : transition.getSource().getId();
        WorkflowState target = transition.getTarget() == null ? null : transition.getTarget().getId();
        log.info("sourceState={} targetState={} event=transition_completed", source, target);
    }

    public void persistCurrentState(Long workflowInstanceId, StateMachine<WorkflowState, WorkflowEvent> stateMachine) {
        if (workflowInstanceId == null || stateMachine.getState() == null) {
            return;
        }
        workflowInstanceRepository.findById(workflowInstanceId).ifPresent(instance -> {
            WorkflowState state = stateMachine.getState().getId();
            instance.setCurrentState(state);
            instance.setStatus(statusFor(state));
            workflowInstanceRepository.save(instance);
            log.info("workflowId={} state={} status={} event=workflow_state_persisted",
                    instance.getId(), state, instance.getStatus());
        });
    }

    private WorkflowStatus statusFor(WorkflowState state) {
        if (state == WorkflowState.PAYMENT_COMPLETED) {
            return WorkflowStatus.COMPLETED;
        }
        if (state == WorkflowState.PAYMENT_FAILED) {
            return WorkflowStatus.FAILED;
        }
        return WorkflowStatus.RUNNING;
    }
}
