package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of FIRCO fraud, AML, and sanctions screening.
 * <p>
 * Outcome probabilities are configurable via {@link MockActionProperties}.
 * In production this would call the actual FIRCO compliance service.
 * A business rejection (sanctions/AML match) is not retried.
 * A technical failure (service unavailable) is eligible for retry.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FircoScreeningAction implements WorkflowAction {

    private final MockActionProperties properties;
    private final RandomGenerator randomGenerator;

    @Override
    public String getActionName() {
        return "FIRCO_SCREENING";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=firco_screening_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());

        double outcome = randomGenerator.nextDouble();
        if (outcome <= properties.fircoApprovedProbability()) {
            log.info("paymentId={} action={} event=firco_screening_approved", context.getPaymentId(), getActionName());
            return ActionResult.success("FIRCO screening approved");
        }
        if (outcome <= properties.fircoApprovedProbability() + properties.fircoRejectedProbability()) {
            log.warn("paymentId={} action={} event=firco_screening_rejected", context.getPaymentId(), getActionName());
            return ActionResult.businessFailure("FIRCO_REJECTED", "Payment rejected by sanctions or AML screening");
        }
        log.warn("paymentId={} action={} event=firco_screening_technical_failure",
                context.getPaymentId(), getActionName());
        return ActionResult.technicalFailure("FIRCO_TECHNICAL_FAILURE",
                "FIRCO screening service temporarily unavailable");
    }
}
