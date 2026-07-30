package com.company.workflow.service;

import com.company.workflow.dto.PaymentContext;
import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.PaymentActionInstance;
import com.company.workflow.entity.PaymentWorkflow;
import com.company.workflow.entity.PaymentWorkflowAction;
import com.company.workflow.entity.PaymentWorkflowInstance;
import com.company.workflow.entity.WorkflowActionStatus;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.factory.ActionFactory;
import com.company.workflow.repository.PaymentActionInstanceRepository;
import com.company.workflow.repository.PaymentWorkflowActionRepository;
import com.company.workflow.repository.PaymentWorkflowInstanceRepository;
import com.company.workflow.repository.PaymentWorkflowRepository;
import com.company.workflow.retry.RetryService;
import com.company.workflow.state.WorkflowEvent;
import com.company.workflow.state.WorkflowState;
import com.company.workflow.util.WorkflowEventResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core orchestration service for the payment workflow.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Start a new payment workflow by loading its action sequence from the database.</li>
 *   <li>Resume an existing workflow after a failure or application restart.</li>
 *   <li>Ensure action execution is idempotent – already-completed actions are skipped.</li>
 *   <li>Persist every state transition and action result via {@link RetryService}
 *       and {@link WorkflowStateMachineService}.</li>
 * </ul>
 * Actions are loaded dynamically from the {@code payment_workflow_action} table at runtime,
 * so the workflow sequence can be changed without code modifications.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWorkflowService {

    private final PaymentWorkflowRepository workflowRepository;
    private final PaymentWorkflowActionRepository workflowActionRepository;
    private final PaymentWorkflowInstanceRepository workflowInstanceRepository;
    private final PaymentActionInstanceRepository actionInstanceRepository;
    private final RetryService retryService;
    private final WorkflowStateMachineService stateMachineService;
    private final WorkflowEventResolver eventResolver;
    private final WorkflowQueryService workflowQueryService;
    private final ActionFactory actionFactory;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts a new payment workflow instance.
     * <p>
     * Loads the action sequence from the database, creates all action instance records
     * in PENDING state, then executes them in sequence.
     * </p>
     *
     * @param request the start workflow request containing payment details
     * @return the completed or failed workflow response
     * @throws WorkflowException if the workflow definition is not found or inactive
     */
    public WorkflowResponse startWorkflow(StartWorkflowRequest request) {
        PaymentWorkflow definition = workflowRepository
                .findByWorkflowNameAndActiveTrue(request.workflowName())
                .orElseThrow(() -> new WorkflowException(
                        "No active workflow definition found for: " + request.workflowName()));

        List<PaymentWorkflowAction> actionDefs =
                workflowActionRepository.findByWorkflowIdOrderByActionSequenceAsc(definition.getId());
        if (actionDefs.isEmpty()) {
            throw new WorkflowException("Workflow '" + request.workflowName() + "' has no actions defined");
        }

        PaymentWorkflowInstance instance = createWorkflowInstance(request);
        String paymentId = resolvePaymentId(request);
        createActionInstances(instance, actionDefs, paymentId);

        PaymentContext context = buildContext(instance, request, paymentId);

        log.info("workflowId={} workflowName={} businessKey={} event=workflow_started",
                instance.getId(), instance.getWorkflowName(), instance.getBusinessKey());

        executeWorkflow(instance, context);
        return workflowQueryService.getWorkflow(instance.getId());
    }

    /**
     * Resumes a FAILED or RUNNING workflow instance from its last unfinished action.
     * <p>
     * Actions that already completed with {@link WorkflowActionStatus#SUCCESS} are skipped.
     * This makes the method idempotent and safe to call after an application restart.
     * </p>
     *
     * @param workflowInstanceId the ID of the workflow instance to resume
     * @return the updated workflow response
     * @throws WorkflowException if the workflow instance is not found or is in a terminal state
     */
    @Transactional
    public WorkflowResponse resumeWorkflow(Long workflowInstanceId) {
        PaymentWorkflowInstance instance = workflowInstanceRepository.findById(workflowInstanceId)
                .orElseThrow(() -> new WorkflowException("Workflow instance not found: " + workflowInstanceId));

        if (instance.getStatus() == WorkflowStatus.COMPLETED) {
            throw new WorkflowException("Workflow " + workflowInstanceId + " is already completed");
        }

        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setWorkflowRetryCount(instance.getWorkflowRetryCount() + 1);
        workflowInstanceRepository.save(instance);

        // Reset FAILED action instances so they can be re-executed
        List<PaymentActionInstance> actionInstances =
                actionInstanceRepository.findByWorkflowInstanceIdOrderByActionOrderAsc(workflowInstanceId);
        resetFailedActions(actionInstances);

        PaymentContext context = buildResumeContext(instance, actionInstances);

        log.info("workflowId={} workflowRetryCount={} event=workflow_resumed",
                workflowInstanceId, instance.getWorkflowRetryCount());

        executeWorkflow(instance, context);
        return workflowQueryService.getWorkflow(workflowInstanceId);
    }

    // ── Internal execution ────────────────────────────────────────────────────

    /**
     * Core execution loop: starts the state machine, fires START_PAYMENT, then iterates
     * over action instances and executes each pending one.
     */
    private void executeWorkflow(PaymentWorkflowInstance instance, PaymentContext context) {
        StateMachine<WorkflowState, WorkflowEvent> stateMachine =
                stateMachineService.start(instance.getId());
        try {
            stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.START_PAYMENT);
            List<PaymentActionInstance> actionInstances =
                    actionInstanceRepository.findByWorkflowInstanceIdOrderByActionOrderAsc(instance.getId());
            runPendingActions(instance.getId(), actionInstances, context, stateMachine);
        } finally {
            stateMachineService.stop(stateMachine);
        }
    }

    /**
     * Iterates over action instances and executes those that are not already done.
     * Already-SUCCESS actions are skipped (idempotency / resumability).
     */
    private void runPendingActions(
            Long workflowInstanceId,
            List<PaymentActionInstance> actionInstances,
            PaymentContext context,
            StateMachine<WorkflowState, WorkflowEvent> stateMachine) {

        for (PaymentActionInstance actionInstance : actionInstances) {
            // Skip already-completed actions (resume after restart / retry idempotency)
            if (actionInstance.getStatus() == WorkflowActionStatus.SUCCESS) {
                log.info("workflowId={} action={} event=action_skipped reason=already_completed",
                        workflowInstanceId, actionInstance.getActionName());
                continue;
            }
            // Skip business failures - they cannot be retried
            if (actionInstance.getStatus() == WorkflowActionStatus.BUSINESS_FAILURE) {
                log.info("workflowId={} action={} event=action_skipped reason=business_failure",
                        workflowInstanceId, actionInstance.getActionName());
                stateMachineService.sendEvent(stateMachine, workflowInstanceId,
                        eventResolver.failureEvent(actionInstance.getActionName()));
                break;
            }

            String actionName = actionInstance.getActionName();
            log.info("workflowId={} currentState={} action={} retryCount={} event=action_started",
                    workflowInstanceId, stateMachine.getState().getId(), actionName,
                    actionInstance.getRetryCount());

            try {
                var result = retryService.executeWithRetry(
                        actionFactory.getAction(actionName), context, actionInstance);
                context = context.toBuilder().responsePayload(result.getResponsePayload()).build();
                stateMachineService.sendEvent(stateMachine, workflowInstanceId,
                        eventResolver.successEvent(actionName));

            } catch (RetryLimitExceededException ex) {
                log.error("workflowId={} action={} event=retry_exhausted reason={}",
                        workflowInstanceId, actionName, ex.getMessage());
                stateMachineService.sendEvent(stateMachine, workflowInstanceId,
                        eventResolver.failureEvent(actionName));
                break;

            } catch (WorkflowException ex) {
                log.error("workflowId={} action={} event=business_failure reason={}",
                        workflowInstanceId, actionName, ex.getMessage());
                stateMachineService.sendEvent(stateMachine, workflowInstanceId,
                        eventResolver.failureEvent(actionName));
                break;
            }
        }
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    @Transactional
    protected PaymentWorkflowInstance createWorkflowInstance(StartWorkflowRequest request) {
        PaymentWorkflowInstance instance = PaymentWorkflowInstance.builder()
                .workflowName(request.workflowName())
                .businessKey(request.businessKey())
                .currentState(WorkflowState.PAYMENT_RECEIVED)
                .status(WorkflowStatus.RUNNING)
                .workflowRetryCount(0)
                .build();
        return workflowInstanceRepository.save(instance);
    }

    @Transactional
    protected void createActionInstances(
            PaymentWorkflowInstance instance,
            List<PaymentWorkflowAction> actionDefs,
            String paymentId) {
        for (PaymentWorkflowAction actionDef : actionDefs) {
            PaymentActionInstance actionInstance = PaymentActionInstance.builder()
                    .workflowInstance(instance)
                    .paymentId(paymentId)
                    .actionName(actionDef.getActionName())
                    .state(actionDef.getActionName())
                    .actionOrder(actionDef.getActionSequence())
                    .status(WorkflowActionStatus.PENDING)
                    .retryCount(0)
                    .build();
            actionInstanceRepository.save(actionInstance);
        }
    }

    /**
     * Resets FAILED/TECHNICAL_FAILURE action instances back to PENDING so they can
     * be re-executed during a workflow resume.
     */
    private void resetFailedActions(List<PaymentActionInstance> actionInstances) {
        for (PaymentActionInstance ai : actionInstances) {
            if (ai.getStatus() == WorkflowActionStatus.FAILED
                    || ai.getStatus() == WorkflowActionStatus.TECHNICAL_FAILURE
                    || ai.getStatus() == WorkflowActionStatus.RETRYING) {
                ai.setStatus(WorkflowActionStatus.PENDING);
                ai.setRetryCount(0);
                ai.setStartedAt(null);
                ai.setCompletedAt(null);
                ai.setErrorCode(null);
                ai.setErrorMessage(null);
                actionInstanceRepository.save(ai);
            }
        }
    }

    // ── Context builders ──────────────────────────────────────────────────────

    private PaymentContext buildContext(
            PaymentWorkflowInstance instance,
            StartWorkflowRequest request,
            String paymentId) {
        return PaymentContext.builder()
                .workflowId(instance.getId())
                .workflowName(instance.getWorkflowName())
                .businessKey(instance.getBusinessKey())
                .paymentId(paymentId)
                .transactionId(request.transactionId())
                .correlationId(request.correlationId())
                .customerId(request.customerId())
                .sourceAccount(request.sourceAccount())
                .destinationAccount(request.destinationAccount())
                .currency(request.currency())
                .amount(request.amount())
                .paymentType(request.paymentType())
                .channel(request.channel())
                .requestTimestamp(request.requestTimestamp())
                .requestPayload(request.requestPayload())
                .build();
    }

    private PaymentContext buildResumeContext(
            PaymentWorkflowInstance instance,
            List<PaymentActionInstance> actionInstances) {
        // Derive paymentId from the first action instance
        String paymentId = actionInstances.isEmpty() ? instance.getBusinessKey()
                : actionInstances.get(0).getPaymentId();
        return PaymentContext.builder()
                .workflowId(instance.getId())
                .workflowName(instance.getWorkflowName())
                .businessKey(instance.getBusinessKey())
                .paymentId(paymentId)
                .build();
    }

    private String resolvePaymentId(StartWorkflowRequest request) {
        return (request.paymentId() == null || request.paymentId().isBlank())
                ? request.businessKey()
                : request.paymentId();
    }
}
