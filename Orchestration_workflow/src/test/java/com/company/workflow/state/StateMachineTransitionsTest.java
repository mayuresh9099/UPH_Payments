package com.company.workflow.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.workflow.entity.PaymentWorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.repository.PaymentWorkflowInstanceRepository;
import com.company.workflow.service.WorkflowStateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the full success-path state machine transition sequence.
 * Uses the in-memory H2 database with {@code @ActiveProfiles("test")}.
 */
@SpringBootTest
@ActiveProfiles("test")
class StateMachineTransitionsTest {

    @Autowired
    private WorkflowStateMachineService stateMachineService;

    @Autowired
    private PaymentWorkflowInstanceRepository workflowInstanceRepository;

    @Test
    void followsSuccessfulTransitionPath() {
        PaymentWorkflowInstance instance = workflowInstanceRepository.save(
                PaymentWorkflowInstance.builder()
                        .workflowName("PAYMENT")
                        .businessKey("BK-SM")
                        .currentState(WorkflowState.PAYMENT_RECEIVED)
                        .status(WorkflowStatus.RUNNING)
                        .build());

        StateMachine<WorkflowState, WorkflowEvent> stateMachine =
                stateMachineService.start(instance.getId());
        try {
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.START_PAYMENT))
                    .isEqualTo(WorkflowState.VALIDATE_PAYMENT);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.VALIDATE_PAYMENT_SUCCESS))
                    .isEqualTo(WorkflowState.FIRCO_SCREENING);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.FIRCO_SCREENING_SUCCESS))
                    .isEqualTo(WorkflowState.FLEX_POSTING);
            assertThat(stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.FLEX_POSTING_SUCCESS))
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
