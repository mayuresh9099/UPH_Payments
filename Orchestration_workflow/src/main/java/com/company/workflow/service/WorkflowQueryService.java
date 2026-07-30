package com.company.workflow.service;

import com.company.workflow.dto.WorkflowActionResponse;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.entity.WorkflowActionEntity;
import com.company.workflow.entity.WorkflowInstance;
import com.company.workflow.exception.WorkflowException;
import com.company.workflow.repository.WorkflowActionRepository;
import com.company.workflow.repository.WorkflowInstanceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowQueryService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowActionRepository workflowActionRepository;

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(Long id) {
        WorkflowInstance instance = workflowInstanceRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow instance not found: " + id));
        List<WorkflowActionResponse> actions = workflowActionRepository
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

    private WorkflowActionResponse toActionResponse(WorkflowActionEntity action) {
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
