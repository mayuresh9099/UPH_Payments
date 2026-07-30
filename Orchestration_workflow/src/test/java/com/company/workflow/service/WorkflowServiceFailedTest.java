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

@SpringBootTest(properties = "mock.actions.payment-posting-success-probability=0.0")
@ActiveProfiles("test")
class WorkflowServiceFailedTest {

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
    void failsWorkflowWhenActionRetriesAreExhausted() {
        WorkflowResponse response = workflowService.startWorkflow(
                request("PAY-FAILED"));

        assertThat(response.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(response.currentState()).isEqualTo(WorkflowState.PAYMENT_FAILED);
        assertThat(response.actions())
                .filteredOn(action -> action.actionName().equals("PAYMENT_POSTING"))
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
