package com.company.workflow.retry;

import com.company.workflow.action.ActionStatus;
import com.company.workflow.action.WorkflowAction;
import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import com.company.workflow.entity.BackoffStrategy;
import com.company.workflow.entity.PaymentActionInstance;
import com.company.workflow.entity.RetryConfiguration;
import com.company.workflow.entity.WorkflowActionStatus;
import com.company.workflow.exception.ActionExecutionException;
import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.PaymentActionInstanceRepository;
import com.company.workflow.repository.RetryConfigurationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes a {@link WorkflowAction} with database-driven retry logic.
 * <p>
 * Retry behaviour is controlled by {@link RetryConfiguration}:
 * <ul>
 *   <li>{@code max_retry} – maximum number of retry attempts.</li>
 *   <li>{@code retry_interval_seconds} – base wait time between retries.</li>
 *   <li>{@code backoff_strategy} – {@link BackoffStrategy#FIXED}, {@link BackoffStrategy#LINEAR},
 *       or {@link BackoffStrategy#EXPONENTIAL}.</li>
 * </ul>
 * Business failures ({@link ActionStatus#BUSINESS_FAILURE}) are never retried.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final RetryConfigurationRepository retryConfigurationRepository;
    private final PaymentActionInstanceRepository paymentActionInstanceRepository;
    private final Sleeper sleeper;

    /**
     * Executes the given action with retry, persisting every status transition.
     *
     * @param action       the action to execute
     * @param context      current payment context
     * @param actionInstance the persisted action instance tracking state
     * @return the successful {@link ActionResult}
     * @throws WorkflowException          on business failure (not retried)
     * @throws RetryLimitExceededException when max retries are exhausted
     */
    @Transactional(noRollbackFor = {RetryLimitExceededException.class, WorkflowException.class})
    public ActionResult executeWithRetry(
            WorkflowAction action,
            PaymentContext context,
            PaymentActionInstance actionInstance) {

        RetryConfiguration config = retryConfigurationRepository.findById(action.getActionName())
                .orElseGet(() -> RetryConfiguration.builder()
                        .actionName(action.getActionName())
                        .maxRetry(0)
                        .retryIntervalSeconds(0)
                        .backoffStrategy(BackoffStrategy.FIXED)
                        .build());

        while (true) {
            try {
                markRunning(actionInstance);
                ActionResult result = action.execute(context);

                if (result.getStatus() == ActionStatus.SUCCESS) {
                    markCompleted(actionInstance);
                    log.info("workflowId={} action={} status=COMPLETED retryCount={}",
                            context.getWorkflowId(), action.getActionName(), actionInstance.getRetryCount());
                    return result;
                }

                if (result.getStatus() == ActionStatus.BUSINESS_FAILURE) {
                    markBusinessFailure(actionInstance, result.getErrorCode(), result.getErrorMessage());
                    throw new WorkflowException("Business failure for action " + action.getActionName()
                            + ": " + result.getErrorMessage());
                }
                // TECHNICAL_FAILURE – fall through to retry logic
                markTechnicalFailure(actionInstance, result.getErrorCode(), result.getErrorMessage());

            } catch (ActionExecutionException ex) {
                markTechnicalFailure(actionInstance, "ACTION_EXECUTION_EXCEPTION", ex.getMessage());
            } catch (RuntimeException ex) {
                if (ex instanceof WorkflowException && !(ex instanceof RetryLimitExceededException)) {
                    throw ex;
                }
                markTechnicalFailure(actionInstance, "UNEXPECTED_ACTION_EXCEPTION", ex.getMessage());
            }

            if (actionInstance.getRetryCount() >= config.getMaxRetry()) {
                actionInstance.setStatus(WorkflowActionStatus.FAILED);
                actionInstance.setCompletedAt(LocalDateTime.now());
                paymentActionInstanceRepository.save(actionInstance);
                throw new RetryLimitExceededException("Retry limit exceeded for action "
                        + action.getActionName() + ": " + actionInstance.getErrorMessage());
            }

            incrementRetry(actionInstance);
            waitForRetry(config, context, action.getActionName(), actionInstance.getRetryCount());
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void markRunning(PaymentActionInstance instance) {
        instance.setStatus(instance.getRetryCount() > 0
                ? WorkflowActionStatus.RETRYING : WorkflowActionStatus.RUNNING);
        if (instance.getStartedAt() == null) {
            instance.setStartedAt(LocalDateTime.now());
        }
        paymentActionInstanceRepository.save(instance);
    }

    private void markCompleted(PaymentActionInstance instance) {
        instance.setStatus(WorkflowActionStatus.SUCCESS);
        instance.setCompletedAt(LocalDateTime.now());
        paymentActionInstanceRepository.save(instance);
    }

    private void markBusinessFailure(PaymentActionInstance instance, String errorCode, String errorMessage) {
        instance.setStatus(WorkflowActionStatus.BUSINESS_FAILURE);
        instance.setErrorCode(errorCode);
        instance.setErrorMessage(errorMessage == null ? "Business failure" : errorMessage);
        instance.setCompletedAt(LocalDateTime.now());
        paymentActionInstanceRepository.save(instance);
    }

    private void markTechnicalFailure(PaymentActionInstance instance, String errorCode, String errorMessage) {
        instance.setStatus(WorkflowActionStatus.TECHNICAL_FAILURE);
        instance.setErrorCode(errorCode);
        instance.setErrorMessage(errorMessage == null ? "Technical failure" : errorMessage);
        paymentActionInstanceRepository.save(instance);
    }

    private void incrementRetry(PaymentActionInstance instance) {
        instance.setRetryCount(instance.getRetryCount() + 1);
        instance.setStatus(WorkflowActionStatus.RETRYING);
        paymentActionInstanceRepository.save(instance);
    }

    /**
     * Waits for the configured retry interval, applying the configured backoff strategy.
     *
     * @param config      retry configuration for the action
     * @param context     payment context (for logging)
     * @param actionName  name of the action being retried
     * @param retryCount  current retry attempt number (1-based)
     */
    private void waitForRetry(
            RetryConfiguration config,
            PaymentContext context,
            String actionName,
            int retryCount) {

        long waitSeconds = computeWait(config, retryCount);
        log.warn("workflowId={} action={} status=RETRYING retryCount={} waitSeconds={} backoff={}",
                context.getWorkflowId(), actionName, retryCount, waitSeconds,
                config.getBackoffStrategy());
        try {
            sleeper.sleep(Duration.ofSeconds(waitSeconds));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ActionExecutionException("Retry wait interrupted for action " + actionName, ex);
        }
    }

    /**
     * Computes the actual wait time in seconds according to the backoff strategy.
     */
    private long computeWait(RetryConfiguration config, int retryCount) {
        long base = config.getRetryIntervalSeconds();
        if (base <= 0) {
            return 0;
        }
        return switch (config.getBackoffStrategy()) {
            case FIXED -> base;
            case LINEAR -> base * retryCount;
            case EXPONENTIAL -> base * (1L << (retryCount - 1)); // base * 2^(retryCount-1)
        };
    }
}
