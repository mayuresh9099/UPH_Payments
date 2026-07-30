package com.company.workflow.dto;

import com.company.workflow.entity.WorkflowStatus;
import com.company.workflow.state.WorkflowState;
import java.time.LocalDateTime;
import java.util.List;

public record WorkflowResponse(
        Long id,
        String workflowName,
        String businessKey,
        WorkflowState currentState,
        WorkflowStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<WorkflowActionResponse> actions
) {
}
