package com.company.workflow.controller;

import com.company.workflow.dto.StartWorkflowRequest;
import com.company.workflow.dto.WorkflowResponse;
import com.company.workflow.service.WorkflowQueryService;
import com.company.workflow.service.WorkflowService;
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

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowQueryService workflowQueryService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse start(@Valid @RequestBody StartWorkflowRequest request) {
        return workflowService.startWorkflow(request);
    }

    @GetMapping("/{id}")
    public WorkflowResponse get(@PathVariable Long id) {
        return workflowQueryService.getWorkflow(id);
    }
}
