package com.company.workflow;

import com.company.workflow.config.WorkflowDefinitionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorkflowDefinitionProperties.class)
public class WorkflowOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowOrchestrationApplication.class, args);
    }
}
