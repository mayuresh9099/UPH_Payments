package com.company.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Payment Workflow Orchestration Framework.
 * <p>
 * Uses Spring Boot, Spring State Machine, Spring Data JPA, and @EnableScheduling
 * for database-driven retry scheduling.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class WorkflowOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowOrchestrationApplication.class, args);
    }
}
