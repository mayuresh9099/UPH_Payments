package com.company.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines a named payment workflow (e.g., "PAYMENT").
 * <p>
 * One {@code PaymentWorkflow} record corresponds to an ordered set of
 * {@link PaymentWorkflowAction} records that specify which actions run and in what sequence.
 * This replaces the previous YAML-based workflow definition approach.
 * </p>
 */
@Entity
@Table(name = "SELECT * FROM orclpdb1.payment_workflow;")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique name used to look up this workflow definition (e.g., "PAYMENT"). */
    @Column(name = "workflow_name", nullable = false, unique = true, length = 100)
    private String workflowName;

    @Column(name = "description", length = 500)
    private String description;

    /** When false the workflow cannot be started. */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
