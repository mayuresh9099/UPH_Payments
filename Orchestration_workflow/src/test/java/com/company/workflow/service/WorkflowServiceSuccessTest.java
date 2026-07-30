package com.company.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.RetryConfiguration;
import com.company.workflow.entity.WorkflowStatus;
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
    private WorkflowService workflowService;

    @Autowired
    private RetryConfigurationRepository retryConfigurationRepository;

    @BeforeEach
    void setUp() {
        retryConfigurationRepository.save(new RetryConfiguration("REQUEST_VALIDATION", 0, 0));
        retryConfigurationRepository.save(new RetryConfiguration("FIRCO_SCREENING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("PAYMENT_POSTING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("ACCOUNTING", 1, 0));
        retryConfigurationRepository.save(new RetryConfiguration("NOTIFICATION", 1, 0));
    }

    @Test
    void completesPaymentWorkflow() {
        WorkflowResponse response = workflowService.startWorkflow(
                request("PAY-SUCCESS"));

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
