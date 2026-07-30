package com.company.workflow.retry;

import com.company.workflow.action.WorkflowAction;
import com.company.workflow.action.ActionStatus;
import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import com.company.workflow.entity.RetryConfiguration;
import com.company.workflow.entity.WorkflowActionEntity;
import com.company.workflow.entity.WorkflowActionStatus;
import com.company.workflow.exception.ActionExecutionException;
import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.RetryConfigurationRepository;
import com.company.workflow.repository.WorkflowActionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final RetryConfigurationRepository retryConfigurationRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final Sleeper sleeper;

    @Transactional(noRollbackFor = {RetryLimitExceededException.class, WorkflowException.class})
    public ActionResult executeWithRetry(WorkflowAction action, PaymentContext context, WorkflowActionEntity actionEntity) {
        RetryConfiguration configuration = retryConfigurationRepository.findById(action.getActionName())
                .orElseGet(() -> RetryConfiguration.builder()
                        .actionName(action.getActionName())
                        .maxRetry(0)
                        .retryIntervalSeconds(0)
                        .build());

        while (true) {
            try {
                markRunning(actionEntity);
                ActionResult result = action.execute(context);
                if (result.getStatus() == ActionStatus.SUCCESS) {
                    markCompleted(actionEntity);
                    log.info("workflowId={} action={} status=COMPLETED retryCount={}",
                            context.getWorkflowId(), action.getActionName(), actionEntity.getRetryCount());
                    return result;
                }
                if (result.getStatus() == ActionStatus.BUSINESS_FAILURE) {
                    markBusinessFailure(actionEntity, result.getErrorCode(), result.getErrorMessage());
                    throw new WorkflowException("Business failure for action " + action.getActionName()
                            + ": " + result.getErrorMessage());
                }
                markTechnicalFailure(actionEntity, result.getErrorCode(), result.getErrorMessage());
            } catch (ActionExecutionException ex) {
                markTechnicalFailure(actionEntity, "ACTION_EXECUTION_EXCEPTION", ex.getMessage());
            } catch (RuntimeException ex) {
                if (ex instanceof WorkflowException && !(ex instanceof RetryLimitExceededException)) {
                    throw ex;
                }
                markTechnicalFailure(actionEntity, "UNEXPECTED_ACTION_EXCEPTION", ex.getMessage());
            }

            if (actionEntity.getRetryCount() >= configuration.getMaxRetry()) {
                actionEntity.setStatus(WorkflowActionStatus.FAILED);
                actionEntity.setCompletedAt(LocalDateTime.now());
                workflowActionRepository.save(actionEntity);
                throw new RetryLimitExceededException("Retry limit exceeded for action "
                        + action.getActionName() + ": " + actionEntity.getErrorMessage());
            }

            incrementRetry(actionEntity);
            waitForRetry(configuration, context, action.getActionName(), actionEntity.getRetryCount());
        }
    }

    private void markRunning(WorkflowActionEntity actionEntity) {
        actionEntity.setStatus(actionEntity.getRetryCount() > 0 ? WorkflowActionStatus.RETRYING : WorkflowActionStatus.RUNNING);
        if (actionEntity.getStartedAt() == null) {
            actionEntity.setStartedAt(LocalDateTime.now());
        }
        workflowActionRepository.save(actionEntity);
    }

    private void markCompleted(WorkflowActionEntity actionEntity) {
        actionEntity.setStatus(WorkflowActionStatus.SUCCESS);
        actionEntity.setCompletedAt(LocalDateTime.now());
        workflowActionRepository.save(actionEntity);
    }

    private void markBusinessFailure(WorkflowActionEntity actionEntity, String errorCode, String errorMessage) {
        actionEntity.setStatus(WorkflowActionStatus.BUSINESS_FAILURE);
        actionEntity.setErrorCode(errorCode);
        actionEntity.setErrorMessage(errorMessage == null ? "Business failure" : errorMessage);
        actionEntity.setCompletedAt(LocalDateTime.now());
        workflowActionRepository.save(actionEntity);
    }

    private void markTechnicalFailure(WorkflowActionEntity actionEntity, String errorCode, String errorMessage) {
        actionEntity.setStatus(WorkflowActionStatus.TECHNICAL_FAILURE);
        actionEntity.setErrorCode(errorCode);
        actionEntity.setErrorMessage(errorMessage == null ? "Technical failure" : errorMessage);
        workflowActionRepository.save(actionEntity);
    }

    private void incrementRetry(WorkflowActionEntity actionEntity) {
        actionEntity.setRetryCount(actionEntity.getRetryCount() + 1);
        actionEntity.setStatus(WorkflowActionStatus.RETRYING);
        workflowActionRepository.save(actionEntity);
    }

    private void waitForRetry(
            RetryConfiguration configuration,
            PaymentContext context,
            String actionName,
            int retryCount
    ) {
        log.warn("workflowId={} action={} status=RETRYING retryCount={} intervalSeconds={} reason={}",
                context.getWorkflowId(), actionName, retryCount,
                configuration.getRetryIntervalSeconds(), "previous attempt failed");
        try {
            sleeper.sleep(Duration.ofSeconds(configuration.getRetryIntervalSeconds()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ActionExecutionException("Retry wait interrupted for action " + actionName, ex);
        }
    }
}
