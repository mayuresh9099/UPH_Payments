package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestValidationAction implements WorkflowAction {

    @Override
    public String getActionName() {
        return "REQUEST_VALIDATION";
    }

    @Override
    public ActionResult execute(PaymentContext context) {
        log.info("paymentId={} transactionId={} action={} event=request_validation_started",
                context.getPaymentId(), context.getTransactionId(), getActionName());
        if (isBlank(context.getPaymentId())) {
            return ActionResult.businessFailure("PAYMENT_ID_MISSING", "Payment ID is mandatory");
        }
        if (isBlank(context.getSourceAccount())) {
            return ActionResult.businessFailure("SOURCE_ACCOUNT_MISSING", "Source account is mandatory");
        }
        if (isBlank(context.getDestinationAccount())) {
            return ActionResult.businessFailure("DESTINATION_ACCOUNT_MISSING", "Destination account is mandatory");
        }
        if (isBlank(context.getCurrency())) {
            return ActionResult.businessFailure("CURRENCY_MISSING", "Currency is mandatory");
        }
        if (context.getAmount() == null || context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ActionResult.businessFailure("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        return ActionResult.success("Payment request validation passed");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
