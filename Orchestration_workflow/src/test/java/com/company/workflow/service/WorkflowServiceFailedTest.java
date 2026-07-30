package com.company.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.PaymentWorkflow;
import com.company.workflow.entity.PaymentWorkflowAction;
import com.company.workflow.entity.RetryConfiguration;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.repository.PaymentWorkflowActionRepository;
import com.company.workflow.repository.PaymentWorkflowRepository;
import com.company.workflow.repository.RetryConfigurationRepository;
import com.company.workflow.state.WorkflowState;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "mock.actions.flex-posting-success-probability=0.0")
@ActiveProfiles("test")
class WorkflowServiceFailedTest {

    @Autowired
    private PaymentWorkflowService paymentWorkflowService;

    @Autowired
    private RetryConfigurationRepository retryConfigurationRepository;

    @Autowired
    private PaymentWorkflowRepository paymentWorkflowRepository;

    @Autowired
    private PaymentWorkflowActionRepository paymentWorkflowActionRepository;

    @BeforeEach
    void setUp() {
        retryConfigurationRepository.save(new RetryConfiguration("VALIDATE_PAYMENT", 0, 0));
        retryConfigurationRepository.save(new RetryConfiguration("FIRCO_SCREENING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("FLEX_POSTING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("ACCOUNTING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("NOTIFICATION", 1, 0));

        PaymentWorkflow workflow = paymentWorkflowRepository
                .findByWorkflowNameAndActiveTrue("PAYMENT")
                .orElseGet(() -> paymentWorkflowRepository.save(
                        PaymentWorkflow.builder()
                                .workflowName("PAYMENT")
                                .description("Standard payment workflow")
                                .active(true)
                                .build()));

        if (paymentWorkflowActionRepository
                .findByWorkflowIdOrderByActionSequenceAsc(workflow.getId()).isEmpty()) {
            String[] actions = {
                "VALIDATE_PAYMENT", "FIRCO_SCREENING", "FLEX_POSTING", "ACCOUNTING", "NOTIFICATION"
            };
            for (int i = 0; i < actions.length; i++) {
                paymentWorkflowActionRepository.save(PaymentWorkflowAction.builder()
                        .workflow(workflow)
                        .actionName(actions[i])
                        .actionSequence(i + 1)
                        .build());
            }
        }
    }

    @Test
    void failsWorkflowWhenActionRetriesAreExhausted() {
        WorkflowResponse response = paymentWorkflowService.startWorkflow(request("PAY-FAILED"));

        assertThat(response.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(response.currentState()).isEqualTo(WorkflowState.PAYMENT_FAILED);
        assertThat(response.actions())
                .filteredOn(action -> action.actionName().equals("FLEX_POSTING"))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.status().name()).isEqualTo("FAILED");
                    assertThat(action.retryCount()).isEqualTo(1);
                });
    }

    private StartWorkflowRequest request(String paymentId) {
        return new StartWorkflowRequest(
                "PAYMENT",
                paymentId,
                paymentId,
                "TXN-" + paymentId,
                "CORR-" + paymentId,
                "CUST-1",
                "SRC-1",
                "DST-1",
                "AED",
                BigDecimal.TEN,
                "DOMESTIC",
                "API",
                LocalDateTime.now(),
                null
        );
    }
}
