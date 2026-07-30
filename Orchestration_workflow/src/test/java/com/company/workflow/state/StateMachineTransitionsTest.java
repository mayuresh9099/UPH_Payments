package com.company.workflow.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.workflow.entity.WorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.repository.WorkflowInstanceRepository;
import com.company.workflow.service.WorkflowStateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StateMachineTransitionsTest {

    @Autowired
    private WorkflowStateMachineService stateMachineService;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Test
    void followsSuccessfulTransitionPath() {
        WorkflowInstance instance = workflowInstanceRepository.save(WorkflowInstance.builder()
                .workflowName("PAYMENT")
                .businessKey("BK-SM")
                .currentState(WorkflowState.PAYMENT_RECEIVED)
                .status(WorkflowStatus.RUNNING)
                .build());

        StateMachine<WorkflowState, WorkflowEvent> stateMachine = stateMachineService.start(instance.getId());
        try {
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.START_PAYMENT))
                    .isEqualTo(WorkflowState.REQUEST_VALIDATION);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.REQUEST_VALIDATION_SUCCESS))
                    .isEqualTo(WorkflowState.FIRCO_SCREENING);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.FIRCO_SCREENING_SUCCESS))
                    .isEqualTo(WorkflowState.PAYMENT_POSTING);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.PAYMENT_POSTING_SUCCESS))
                    .isEqualTo(WorkflowState.ACCOUNTING);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.ACCOUNTING_SUCCESS))
                    .isEqualTo(WorkflowState.NOTIFICATION);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.NOTIFICATION_SUCCESS))
                    .isEqualTo(WorkflowState.PAYMENT_COMPLETED);
        } finally {
            stateMachineService.stop(stateMachine);
        }
    }
}
