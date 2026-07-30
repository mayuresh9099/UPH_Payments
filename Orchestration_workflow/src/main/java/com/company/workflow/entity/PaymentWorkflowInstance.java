package com.company.workflow.entity;

import com.company.workflow.state.WorkflowState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single running or completed instance of a payment workflow.
 * <p>
 * Persisted state ensures the workflow is resumable after an application restart:
 * the current state and individual action statuses are stored in the database
 * and can be re-loaded to continue execution.
 * </p>
 */
@Entity
@Table(name = "payment_workflow_instance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_name", nullable = false, length = 100)
    private String workflowName;

    @Column(name = "business_key", nullable = false, length = 100)
    private String businessKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 50)
    private WorkflowState currentState;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WorkflowStatus status;

    /**
     * Tracks how many times this workflow instance has been retried as a whole
     * (via the retry endpoint or the scheduler).
     */
    @Builder.Default
    @Column(name = "workflow_retry_count", nullable = false)
    private Integer workflowRetryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
