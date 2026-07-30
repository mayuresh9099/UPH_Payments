package com.company.workflow.repository;

import com.company.workflow.entity.WorkflowActionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowActionRepository extends JpaRepository<WorkflowActionEntity, Long> {

    List<WorkflowActionEntity> findByWorkflowInstanceIdOrderByActionOrderAsc(Long workflowInstanceId);
}
