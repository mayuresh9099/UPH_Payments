package com.company.workflow.action;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.actions")
public record MockActionProperties(
        double paymentPostingSuccessProbability,
        double accountingSuccessProbability,
        double notificationSuccessProbability,
        double fircoApprovedProbability,
        double fircoRejectedProbability
) {
}
