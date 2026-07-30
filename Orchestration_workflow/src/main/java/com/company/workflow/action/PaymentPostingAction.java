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
public class PaymentPostingAction implements WorkflowAction {

    private final MockActionProperties properties;
    private final RandomGenerator randomGenerator;

    @Override
    public String getActionName() {
        return "PAYMENT_POSTING";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=payment_posting_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());
        return randomGenerator.nextDouble() <= properties.paymentPostingSuccessProbability()
                ? ActionResult.success("Payment posting placeholder completed")
                : ActionResult.technicalFailure("POSTING_TEMPORARY_FAILURE", "Core banking posting temporarily failed");
    }
}
