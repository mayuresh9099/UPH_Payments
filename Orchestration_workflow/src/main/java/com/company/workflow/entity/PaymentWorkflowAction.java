package com.company.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines a single action within a {@link PaymentWorkflow}, specifying the action name
 * and its execution sequence.
 * <p>
 * Actions are loaded dynamically at workflow start time, ordered by {@code actionSequence}.
 * To add a new step to the workflow, insert a row here without any code changes.
 * </p>
 */
@Entity
@Table(name = "payment_workflow_action")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWorkflowAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private PaymentWorkflow workflow;

    /** Matches the {@code getActionName()} return value of a registered {@link com.company.workflow.action.WorkflowAction}. */
    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    /** 1-based ordering within the workflow. */
    @Column(name = "action_sequence", nullable = false)
    private Integer actionSequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
