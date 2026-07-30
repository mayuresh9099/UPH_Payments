package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of GL (General Ledger) accounting — debit and credit entries.
 * <p>
 * Success probability is configurable via {@link MockActionProperties}.
 * In production this would post entries to the accounting/GL system.
 * Technical failures are eligible for retry.
 * </p>
 */
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
        if (randomGenerator.nextDouble() <= properties.accountingSuccessProbability()) {
            log.info("paymentId={} action={} event=accounting_completed", context.getPaymentId(), getActionName());
            return ActionResult.success("Accounting placeholder completed");
        }
        log.warn("paymentId={} action={} event=accounting_technical_failure", context.getPaymentId(), getActionName());
        return ActionResult.technicalFailure("ACCOUNTING_TEMPORARY_FAILURE",
                "Accounting posting temporarily failed");
    }
}
