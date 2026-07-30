package com.company.workflow.repository;

import com.company.workflow.entity.PaymentActionInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PaymentActionInstance} action execution audit records.
 */
@Repository
public interface PaymentActionInstanceRepository extends JpaRepository<PaymentActionInstance, Long> {

    /**
     * Returns all action instances for the given workflow instance, ordered for execution.
     *
     * @param workflowInstanceId the ID of the parent {@link com.company.workflow.entity.PaymentWorkflowInstance}
     * @return action instances sorted by {@code actionOrder} ascending
     */
    List<PaymentActionInstance> findByWorkflowInstanceIdOrderByActionOrderAsc(Long workflowInstanceId);
}
