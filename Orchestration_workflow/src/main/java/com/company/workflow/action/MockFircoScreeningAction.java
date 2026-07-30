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
public class MockFircoScreeningAction implements WorkflowAction {

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
            return ActionResult.success("FIRCO screening approved");
        }
        if (outcome <= properties.fircoApprovedProbability() + properties.fircoRejectedProbability()) {
            return ActionResult.businessFailure("FIRCO_REJECTED", "Payment rejected by sanctions or AML screening");
        }
        return ActionResult.technicalFailure("FIRCO_TECHNICAL_FAILURE", "FIRCO screening service temporarily unavailable");
    }
}
