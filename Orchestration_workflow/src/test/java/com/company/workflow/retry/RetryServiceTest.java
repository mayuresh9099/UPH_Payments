package com.company.workflow.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.workflow.action.WorkflowAction;
import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import com.company.workflow.entity.RetryConfiguration;
import com.company.workflow.entity.WorkflowActionEntity;
import com.company.workflow.entity.WorkflowActionStatus;
import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.RetryConfigurationRepository;
import com.company.workflow.repository.WorkflowActionRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RetryServiceTest {

    private final RetryConfigurationRepository retryConfigurationRepository = mock(RetryConfigurationRepository.class);
    private final WorkflowActionRepository workflowActionRepository = mock(WorkflowActionRepository.class);
    private final Sleeper sleeper = mock(Sleeper.class);
    private final RetryService retryService =
            new RetryService(retryConfigurationRepository, workflowActionRepository, sleeper);

    @Test
    void retriesFailedActionAndReturnsSuccess() throws Exception {
        WorkflowAction action = mock(WorkflowAction.class);
        PaymentContext context = PaymentContext.builder().workflowId(10L).businessKey("BK-1").paymentId("PAY-1").build();
        WorkflowActionEntity actionEntity = WorkflowActionEntity.builder()
                .paymentId("PAY-1")
                .actionName("PAYMENT_POSTING")
                .state("PAYMENT_POSTING")
                .retryCount(0)
                .status(WorkflowActionStatus.PENDING)
                .build();

        when(action.getActionName()).thenReturn("PAYMENT_POSTING");
        when(retryConfigurationRepository.findById("PAYMENT_POSTING"))
                .thenReturn(Optional.of(new RetryConfiguration("PAYMENT_POSTING", 2, 0)));
        when(action.execute(context))
                .thenReturn(ActionResult.technicalFailure("TEMPORARY_FAILURE", "temporary failure"))
                .thenReturn(ActionResult.success("posted"));

        ActionResult result = retryService.executeWithRetry(action, context, actionEntity);

        assertThat(result.getStatus().name()).isEqualTo("SUCCESS");
        assertThat(actionEntity.getRetryCount()).isEqualTo(1);
        assertThat(actionEntity.getStatus()).isEqualTo(WorkflowActionStatus.SUCCESS);
        verify(sleeper).sleep(Duration.ZERO);
        verify(action, times(2)).execute(context);
    }

    @Test
    void throwsWhenRetryLimitIsExceeded() throws Exception {
        WorkflowAction action = mock(WorkflowAction.class);
        PaymentContext context = PaymentContext.builder().workflowId(10L).businessKey("BK-1").paymentId("PAY-1").build();
        WorkflowActionEntity actionEntity = WorkflowActionEntity.builder()
                .paymentId("PAY-1")
                .actionName("FIRCO_SCREENING")
                .state("FIRCO_SCREENING")
                .retryCount(0)
                .status(WorkflowActionStatus.PENDING)
                .build();

        when(action.getActionName()).thenReturn("FIRCO_SCREENING");
        when(retryConfigurationRepository.findById("FIRCO_SCREENING"))
                .thenReturn(Optional.of(new RetryConfiguration("FIRCO_SCREENING", 1, 0)));
        when(action.execute(context))
                .thenReturn(ActionResult.technicalFailure("FIRCO_TIMEOUT", "screening timeout"));

        assertThatThrownBy(() -> retryService.executeWithRetry(action, context, actionEntity))
                .isInstanceOf(RetryLimitExceededException.class)
                .hasMessageContaining("Retry limit exceeded");

        assertThat(actionEntity.getRetryCount()).isEqualTo(1);
        assertThat(actionEntity.getStatus()).isEqualTo(WorkflowActionStatus.FAILED);
        verify(action, times(2)).execute(context);
        verify(sleeper).sleep(Duration.ZERO);
        Mockito.verifyNoMoreInteractions(sleeper);
    }

    @Test
    void doesNotRetryBusinessFailure() {
        WorkflowAction action = mock(WorkflowAction.class);
        PaymentContext context = PaymentContext.builder().workflowId(10L).businessKey("BK-1").paymentId("PAY-1").build();
        WorkflowActionEntity actionEntity = WorkflowActionEntity.builder()
                .paymentId("PAY-1")
                .actionName("FIRCO_SCREENING")
                .state("FIRCO_SCREENING")
                .retryCount(0)
                .status(WorkflowActionStatus.PENDING)
                .build();

        when(action.getActionName()).thenReturn("FIRCO_SCREENING");
        when(retryConfigurationRepository.findById("FIRCO_SCREENING"))
                .thenReturn(Optional.of(new RetryConfiguration("FIRCO_SCREENING", 3, 0)));
        when(action.execute(context))
                .thenReturn(ActionResult.businessFailure("FIRCO_REJECTED", "Sanctioned customer"));

        assertThatThrownBy(() -> retryService.executeWithRetry(action, context, actionEntity))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Business failure");

        assertThat(actionEntity.getRetryCount()).isZero();
        assertThat(actionEntity.getStatus()).isEqualTo(WorkflowActionStatus.BUSINESS_FAILURE);
        verify(action).execute(context);
        Mockito.verifyNoInteractions(sleeper);
    }
}
