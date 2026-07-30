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

@SpringBootTest
@ActiveProfiles("test")
class WorkflowServiceSuccessTest {

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
    void completesPaymentWorkflow() {
        WorkflowResponse response = paymentWorkflowService.startWorkflow(request("PAY-SUCCESS"));

        assertThat(response.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(response.currentState()).isEqualTo(WorkflowState.PAYMENT_COMPLETED);
        assertThat(response.actions()).hasSize(5);
        assertThat(response.actions()).allMatch(action -> action.status().name().equals("SUCCESS"));
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
