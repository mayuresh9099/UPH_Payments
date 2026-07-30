package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of Flexcube (CBS / core banking) payment posting.
 * <p>
 * Success probability is configurable via {@link MockActionProperties}.
 * In production this would integrate with Flexcube or the core banking system.
 * Technical failures are eligible for retry; no business-failure path exists here.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlexPostingAction implements WorkflowAction {

    private final MockActionProperties properties;
    private final RandomGenerator randomGenerator;

    @Override
    public String getActionName() {
        return "FLEX_POSTING";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=flex_posting_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());

        if (randomGenerator.nextDouble() <= properties.flexPostingSuccessProbability()) {
            log.info("paymentId={} action={} event=flex_posting_completed", context.getPaymentId(), getActionName());
            return ActionResult.success("Flexcube payment posting placeholder completed");
        }
        log.warn("paymentId={} action={} event=flex_posting_technical_failure", context.getPaymentId(), getActionName());
        return ActionResult.technicalFailure("FLEX_POSTING_TEMPORARY_FAILURE",
                "Core banking (Flexcube) posting temporarily failed");
    }
}
