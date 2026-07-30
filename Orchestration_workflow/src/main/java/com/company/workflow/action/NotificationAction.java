package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of payment notification delivery (SMS, email, Kafka, push).
 * <p>
 * Success probability is configurable via {@link MockActionProperties}.
 * In production this would trigger real notification channels.
 * Technical failures are eligible for retry.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAction implements WorkflowAction {

    private final MockActionProperties properties;
    private final RandomGenerator randomGenerator;

    @Override
    public String getActionName() {
        return "NOTIFICATION";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=notification_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());
        if (randomGenerator.nextDouble() <= properties.notificationSuccessProbability()) {
            log.info("paymentId={} action={} event=notification_sent", context.getPaymentId(), getActionName());
            return ActionResult.success("Notification placeholder completed");
        }
        log.warn("paymentId={} action={} event=notification_technical_failure",
                context.getPaymentId(), getActionName());
        return ActionResult.technicalFailure("NOTIFICATION_TEMPORARY_FAILURE",
                "Notification delivery temporarily failed");
    }
}
