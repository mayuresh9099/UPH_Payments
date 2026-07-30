package com.company.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StartWorkflowRequest(
        @NotBlank String workflowName,
        @NotBlank String businessKey,
        String paymentId,
        String transactionId,
        String correlationId,
        String customerId,
        String sourceAccount,
        String destinationAccount,
        String currency,
        BigDecimal amount,
        String paymentType,
        String channel,
        LocalDateTime requestTimestamp,
        Object requestPayload
) {
}
