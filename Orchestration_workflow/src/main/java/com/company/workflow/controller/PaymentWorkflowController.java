package com.company.workflow.controller;

import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.service.PaymentWorkflowService;
import com.company.workflow.service.WorkflowQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Payment Workflow Orchestration API.
 *
 * <pre>
 * POST /api/payments/workflows/start           – start a new payment workflow
 * GET  /api/payments/workflows/{workflowId}    – retrieve workflow audit state
 * POST /api/payments/workflows/{workflowId}/retry – manually retry a failed workflow
 * </pre>
 */
@RestController
@RequestMapping("/api/payments/workflows")
@RequiredArgsConstructor
public class PaymentWorkflowController {

    private final PaymentWorkflowService paymentWorkflowService;
    private final WorkflowQueryService workflowQueryService;

    /**
     * Starts a new payment workflow for the given payment details.
     *
     * @param request start request containing payment and workflow metadata
     * @return the completed or failed workflow response with action audit trail
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse start(@Valid @RequestBody StartWorkflowRequest request) {
        return paymentWorkflowService.startWorkflow(request);
    }

    /**
     * Returns the current state and full action audit trail for the given workflow instance.
     *
     * @param workflowId the database ID of the workflow instance
     * @return the workflow response DTO
     */
    @GetMapping("/{workflowId}")
    public WorkflowResponse get(@PathVariable Long workflowId) {
        return workflowQueryService.getWorkflow(workflowId);
    }

    /**
     * Manually retries a FAILED payment workflow, resuming from the last failed action.
     *
     * @param workflowId the database ID of the workflow instance to retry
     * @return the updated workflow response after retry execution
     */
    @PostMapping("/{workflowId}/retry")
    public WorkflowResponse retry(@PathVariable Long workflowId) {
        return paymentWorkflowService.resumeWorkflow(workflowId);
    }
}
