package com.company.workflow.dto;

import com.company.workflow.entity.WorkflowActionStatus;
import java.time.LocalDateTime;

public record WorkflowActionResponse(
        Long id,
        String paymentId,
        String actionName,
        String state,
        Integer actionOrder,
        WorkflowActionStatus status,
        Integer retryCount,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
