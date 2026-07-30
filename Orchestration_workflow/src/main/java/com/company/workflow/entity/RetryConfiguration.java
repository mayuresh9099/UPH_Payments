package com.company.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
