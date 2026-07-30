package com.company.workflow.repository;

import com.company.workflow.entity.PaymentWorkflow;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PaymentWorkflow} workflow definitions.
 */
@Repository
public interface PaymentWorkflowRepository extends JpaRepository<PaymentWorkflow, Long> {

    Optional<PaymentWorkflow> findByWorkflowNameAndActiveTrue(String workflowName);
}
