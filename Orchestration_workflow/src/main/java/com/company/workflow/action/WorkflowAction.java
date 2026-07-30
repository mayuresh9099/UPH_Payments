package com.company.workflow.action;

import com.company.workflow.dto.ActionResult;
import com.company.workflow.dto.PaymentContext;

public interface WorkflowAction {

    String getActionName();

    ActionResult execute(PaymentContext context);
}
