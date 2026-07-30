package com.company.workflow.service;

import com.company.workflow.action.WorkflowAction;
import com.company.workflow.action.WorkflowActionRegistry;
import com.company.workflow.config.WorkflowDefinitionProperties;
import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.WorkflowActionEntity;
import com.company.workflow.entity.WorkflowActionStatus;
import com.company.workflow.entity.WorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.exception.RetryLimitExceededException;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.WorkflowActionRepository;
import com.company.workflow.repository.WorkflowInstanceRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionProperties workflowDefinitionProperties;
    private final WorkflowActionRegistry actionRegistry;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final RetryService retryService;
    private final WorkflowStateMachineService stateMachineService;
    private final WorkflowEventResolver eventResolver;
    private final WorkflowQueryService workflowQueryService;

    public WorkflowResponse startWorkflow(StartWorkflowRequest request) {
        WorkflowInstance instance = createWorkflowInstance(request);
        List<String> actionNames = workflowDefinitionProperties.actionsFor(request.workflowName());
        createActionRows(instance, actionNames, paymentId(request));

        PaymentContext context = PaymentContext.builder()
                .workflowId(instance.getId())
                .workflowName(instance.getWorkflowName())
                .businessKey(instance.getBusinessKey())
                .paymentId(paymentId(request))
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

        log.info("workflowId={} workflowName={} businessKey={} event=workflow_started",
                instance.getId(), instance.getWorkflowName(), instance.getBusinessKey());

        StateMachine<WorkflowState, WorkflowEvent> stateMachine = stateMachineService.start(instance.getId());
        try {
            stateMachineService.sendEvent(stateMachine, instance.getId(), WorkflowEvent.START_PAYMENT);
            executeActions(instance.getId(), actionNames, context, stateMachine);
        } finally {
            stateMachineService.stop(stateMachine);
        }
        return workflowQueryService.getWorkflow(instance.getId());
    }

    private void executeActions(
            Long workflowInstanceId,
            List<String> actionNames,
            PaymentContext context,
            StateMachine<WorkflowState, WorkflowEvent> stateMachine
    ) {
        List<WorkflowActionEntity> actionRows =
                workflowActionRepository.findByWorkflowInstanceIdOrderByActionOrderAsc(workflowInstanceId);
        for (WorkflowActionEntity actionRow : actionRows) {
            String actionName = actionRow.getActionName();
            WorkflowAction action = actionRegistry.get(actionName);
            log.info("workflowId={} currentState={} action={} retryCount={} event=action_started",
                    workflowInstanceId, stateMachine.getState().getId(), actionName, actionRow.getRetryCount());
            try {
                ActionResult result = retryService.executeWithRetry(action, context, actionRow);
                context = context.toBuilder().responsePayload(result.getResponsePayload()).build();
                stateMachineService.sendEvent(stateMachine, workflowInstanceId, eventResolver.successEvent(actionName));
            } catch (RetryLimitExceededException ex) {
                log.error("workflowId={} action={} event=retry_exhausted reason={}",
                        workflowInstanceId, actionName, ex.getMessage());
                stateMachineService.sendEvent(stateMachine, workflowInstanceId, eventResolver.failureEvent(actionName));
                break;
            } catch (WorkflowException ex) {
                log.error("workflowId={} action={} event=business_failure reason={}",
                        workflowInstanceId, actionName, ex.getMessage());
                stateMachineService.sendEvent(stateMachine, workflowInstanceId, eventResolver.failureEvent(actionName));
                break;
            }
        }
    }

    @Transactional
    protected WorkflowInstance createWorkflowInstance(StartWorkflowRequest request) {
        WorkflowInstance instance = WorkflowInstance.builder()
                .workflowName(request.workflowName())
                .businessKey(request.businessKey())
                .currentState(WorkflowState.PAYMENT_RECEIVED)
                .status(WorkflowStatus.RUNNING)
                .build();
        return workflowInstanceRepository.save(instance);
    }

    @Transactional
    protected void createActionRows(WorkflowInstance instance, List<String> actionNames, String paymentId) {
        for (int index = 0; index < actionNames.size(); index++) {
            WorkflowActionEntity action = WorkflowActionEntity.builder()
                    .workflowInstance(instance)
                    .paymentId(paymentId)
                    .actionName(actionNames.get(index))
                    .state(actionNames.get(index))
                    .actionOrder(index + 1)
                    .status(WorkflowActionStatus.PENDING)
                    .retryCount(0)
                    .build();
            workflowActionRepository.save(action);
        }
    }

    private String paymentId(StartWorkflowRequest request) {
        return request.paymentId() == null || request.paymentId().isBlank()
                ? request.businessKey()
                : request.paymentId();
    }
}
