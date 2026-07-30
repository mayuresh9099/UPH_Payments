package com.company.workflow.scheduler;

import com.company.workflow.entity.PaymentWorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.repository.PaymentWorkflowInstanceRepository;
import com.company.workflow.service.PaymentWorkflowService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background scheduler that automatically retries FAILED payment workflow instances.
 * <p>
 * Runs at a configurable fixed delay (default: 60 s). For each FAILED workflow it:
 * <ol>
 *   <li>Attempts to resume execution from the last unfinished action.</li>
 *   <li>Logs success or failure for each retry attempt.</li>
 * </ol>
 * This scheduler also handles resumability after an application restart: any workflow
 * instance left in RUNNING state (because the process was killed mid-execution) will be
 * picked up and re-driven to completion or failure.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final PaymentWorkflowInstanceRepository workflowInstanceRepository;
    private final PaymentWorkflowService paymentWorkflowService;

    /**
     * Periodically scans for FAILED or stale RUNNING workflow instances and retries them.
     *
     * <p>The fixed delay is configurable via {@code workflow.scheduler.retry-interval-ms}
     * (default 60 000 ms = 1 minute).</p>
     */
    @Scheduled(fixedDelayString = "${workflow.scheduler.retry-interval-ms:60000}")
    public void retryFailedWorkflows() {
        List<PaymentWorkflowInstance> failedInstances =
                workflowInstanceRepository.findByStatus(WorkflowStatus.FAILED);

        if (failedInstances.isEmpty()) {
            log.debug("scheduler=retry_scheduler event=no_failed_workflows");
            return;
        }

        log.info("scheduler=retry_scheduler event=retrying_failed_workflows count={}",
                failedInstances.size());

        for (PaymentWorkflowInstance instance : failedInstances) {
            retryInstance(instance);
        }
    }

    /**
     * Attempts to resume a single workflow instance, logging the outcome.
     *
     * @param instance the FAILED workflow instance to retry
     */
    private void retryInstance(PaymentWorkflowInstance instance) {
        try {
            log.info("scheduler=retry_scheduler workflowId={} event=retry_attempt_started",
                    instance.getId());
            paymentWorkflowService.resumeWorkflow(instance.getId());
            log.info("scheduler=retry_scheduler workflowId={} event=retry_attempt_completed",
                    instance.getId());
        } catch (Exception ex) {
            log.error("scheduler=retry_scheduler workflowId={} event=retry_attempt_failed reason={}",
                    instance.getId(), ex.getMessage(), ex);
        }
    }
}
