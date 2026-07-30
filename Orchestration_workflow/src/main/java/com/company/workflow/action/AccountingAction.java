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
public class AccountingAction implements WorkflowAction {

    private final MockActionProperties properties;
    private final RandomGenerator randomGenerator;

    @Override
    public String getActionName() {
        return "ACCOUNTING";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=accounting_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());
        return randomGenerator.nextDouble() <= properties.accountingSuccessProbability()
                ? ActionResult.success("Accounting placeholder completed")
                : ActionResult.technicalFailure("ACCOUNTING_TEMPORARY_FAILURE", "Accounting posting temporarily failed");
    }
}
