package com.company.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log of a single action execution within a {@link PaymentWorkflowInstance}.
 * <p>
 * One row is created per action per workflow instance at workflow start time with
 * status {@link WorkflowActionStatus#PENDING}. The status transitions as the action
 * runs, retries, or fails.
 * </p>
 */
@Entity
@Table(name = "payment_action_instance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentActionInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private PaymentWorkflowInstance workflowInstance;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    /** The state the state machine is in when this action executes. */
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    /** 1-based ordering matching {@link PaymentWorkflowAction#getActionSequence()}. */
    @Column(name = "action_order", nullable = false)
    private Integer actionOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WorkflowActionStatus status;

    /** Number of retry attempts made so far for this action. */
    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
