package com.company.workflow.repository;

import com.company.workflow.entity.PaymentWorkflowAction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PaymentWorkflowAction} action sequence definitions.
 */
@Repository
public interface PaymentWorkflowActionRepository extends JpaRepository<PaymentWorkflowAction, Long> {

    /**
     * Returns the ordered list of actions for the given workflow definition.
     *
     * @param workflowId the ID of the {@link com.company.workflow.entity.PaymentWorkflow}
     * @return actions sorted by {@code actionSequence} ascending
     */
    List<PaymentWorkflowAction> findByWorkflowIdOrderByActionSequenceAsc(Long workflowId);
}
