package com.company.workflow.repository;

import com.company.workflow.entity.PaymentWorkflowInstance;
import com.company.workflow.entity.WorkflowStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PaymentWorkflowInstance} running and completed workflow instances.
 */
@Repository
public interface PaymentWorkflowInstanceRepository extends JpaRepository<PaymentWorkflowInstance, Long> {

    /**
     * Returns all workflow instances with the given status.
     * Used by the {@link com.company.workflow.scheduler.RetryScheduler} to find FAILED instances.
     *
     * @param status the target workflow status
     * @return list of matching instances
     */
    List<PaymentWorkflowInstance> findByStatus(WorkflowStatus status);
}
