package com.company.workflow.action;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mock probability configuration for downstream action implementations.
 * Configured under {@code mock.actions.*} in application properties.
 */
@ConfigurationProperties(prefix = "mock.actions")
public record MockActionProperties(
        double flexPostingSuccessProbability,
        double accountingSuccessProbability,
        double notificationSuccessProbability,
        double fircoApprovedProbability,
        double fircoRejectedProbability
) {
}
