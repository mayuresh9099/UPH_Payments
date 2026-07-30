package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        return randomGenerator.nextDouble() <= properties.notificationSuccessProbability()
                ? ActionResult.success("Notification placeholder completed")
                : ActionResult.technicalFailure("NOTIFICATION_TEMPORARY_FAILURE", "Notification delivery temporarily failed");
    }
}
