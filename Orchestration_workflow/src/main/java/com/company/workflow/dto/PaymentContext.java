package com.company.workflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder(toBuilder = true)
public class PaymentContext {

    private final Long workflowId;
    private final String workflowName;
    private final String businessKey;
    private final String paymentId;
    private final String transactionId;
    private final String correlationId;
    private final String customerId;
    private final String sourceAccount;
    private final String destinationAccount;
    private final String currency;
    private final BigDecimal amount;
    private final String paymentType;
    private final String channel;
    private final LocalDateTime requestTimestamp;
    private final Object requestPayload;
    private final Object responsePayload;

    @Singular("metadata")
    private final Map<String, Object> metadata;

    public PaymentContext withMetadata(String key, Object value) {
        Map<String, Object> updated = new HashMap<>(metadata == null ? Map.of() : metadata);
        updated.put(key, value);
        return toBuilder().metadata(updated).build();
    }
}
