package com.company.workflow.service;

import com.company.workflow.dto.WorkflowActionResponse;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.PaymentActionInstance;
import com.company.workflow.entity.PaymentWorkflowInstance;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.PaymentActionInstanceRepository;
import com.company.workflow.repository.PaymentWorkflowInstanceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only query service for retrieving workflow instance state and action audit data.
 */
@Service
@RequiredArgsConstructor
public class WorkflowQueryService {

    private final PaymentWorkflowInstanceRepository workflowInstanceRepository;
    private final PaymentActionInstanceRepository actionInstanceRepository;

    /**
     * Returns the full workflow response including all action audit records.
     *
     * @param id the workflow instance ID
     * @return the workflow response DTO
     * @throws WorkflowException if the instance does not exist
     */
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(Long id) {
        PaymentWorkflowInstance instance = workflowInstanceRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow instance not found: " + id));
        List<WorkflowActionResponse> actions = actionInstanceRepository
                .findByWorkflowInstanceIdOrderByActionOrderAsc(id)
                .stream()
                .map(this::toActionResponse)
                .toList();
        return new WorkflowResponse(
                instance.getId(),
                instance.getWorkflowName(),
                instance.getBusinessKey(),
                instance.getCurrentState(),
                instance.getStatus(),
                instance.getCreatedAt(),
                instance.getUpdatedAt(),
                actions
        );
    }

    private WorkflowActionResponse toActionResponse(PaymentActionInstance action) {
        return new WorkflowActionResponse(
                action.getId(),
                action.getPaymentId(),
                action.getActionName(),
                action.getState(),
                action.getActionOrder(),
                action.getStatus(),
                action.getRetryCount(),
                action.getErrorCode(),
                action.getErrorMessage(),
                action.getStartedAt(),
                action.getCompletedAt()
        );
    }
}
