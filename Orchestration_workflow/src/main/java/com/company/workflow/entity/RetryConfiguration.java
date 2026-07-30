package com.company.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database-driven retry policy for a named workflow action.
 * <p>
 * The retry policy is configurable without code changes:
 * <pre>
 * UPDATE retry_configuration
 *   SET max_retry = 5, retry_interval_seconds = 10, backoff_strategy = 'EXPONENTIAL'
 * WHERE action_name = 'FLEX_POSTING';
 * </pre>
 * </p>
 */
@Entity
@Table(name = "retry_configuration")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfiguration {

    @Id
    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry;

    @Column(name = "retry_interval_seconds", nullable = false)
    private Integer retryIntervalSeconds;

    /**
     * Backoff strategy applied between retries. Defaults to {@link BackoffStrategy#FIXED}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "backoff_strategy", nullable = false, length = 20)
    private BackoffStrategy backoffStrategy = BackoffStrategy.FIXED;

    /**
     * Convenience constructor that defaults backoff strategy to {@link BackoffStrategy#FIXED}.
     *
     * @param actionName             the action name (primary key)
     * @param maxRetry               maximum number of retries
     * @param retryIntervalSeconds   base interval in seconds between retries
     */
    public RetryConfiguration(String actionName, Integer maxRetry, Integer retryIntervalSeconds) {
        this(actionName, maxRetry, retryIntervalSeconds, BackoffStrategy.FIXED);
    }
}
