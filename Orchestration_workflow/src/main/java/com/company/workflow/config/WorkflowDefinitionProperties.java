package com.company.workflow.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow")
public record WorkflowDefinitionProperties(Map<String, WorkflowDefinition> definitions) {

    public WorkflowDefinitionProperties {
        definitions = definitions == null ? new LinkedHashMap<>() : definitions;
    }

    public List<String> actionsFor(String workflowName) {
        WorkflowDefinition definition = definitions.get(workflowName);
        if (definition == null || definition.actions() == null || definition.actions().isEmpty()) {
            throw new IllegalArgumentException("No workflow definition found for " + workflowName);
        }
        return definition.actions();
    }

    public record WorkflowDefinition(List<String> actions) {
    }
}
