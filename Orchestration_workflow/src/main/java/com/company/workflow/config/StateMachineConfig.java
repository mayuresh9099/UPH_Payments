package com.company.workflow.config;

import com.company.workflow.listener.WorkflowStateMachineListener;
import com.company.workflow.state.WorkflowEvent;
import com.company.workflow.state.WorkflowState;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<WorkflowState, WorkflowEvent> {

    private final WorkflowStateMachineListener listener;

    @Override
    public void configure(StateMachineConfigurationConfigurer<WorkflowState, WorkflowEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false)
                .listener(listener);
    }

    @Override
    public void configure(StateMachineStateConfigurer<WorkflowState, WorkflowEvent> states) throws Exception {
        states.withStates()
                .initial(WorkflowState.PAYMENT_RECEIVED)
                .states(EnumSet.allOf(WorkflowState.class))
                .end(WorkflowState.PAYMENT_COMPLETED)
                .end(WorkflowState.PAYMENT_FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<WorkflowState, WorkflowEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(WorkflowState.PAYMENT_RECEIVED)
                    .target(WorkflowState.REQUEST_VALIDATION)
                    .event(WorkflowEvent.START_PAYMENT)
                .and()
                .withExternal()
                    .source(WorkflowState.REQUEST_VALIDATION)
                    .target(WorkflowState.FIRCO_SCREENING)
                    .event(WorkflowEvent.REQUEST_VALIDATION_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.REQUEST_VALIDATION)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.REQUEST_VALIDATION_FAILED)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.PAYMENT_POSTING)
                    .event(WorkflowEvent.FIRCO_SCREENING_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FIRCO_SCREENING_FAILED)
                .and()
                .withExternal()
                    .source(WorkflowState.PAYMENT_POSTING)
                    .target(WorkflowState.ACCOUNTING)
                    .event(WorkflowEvent.PAYMENT_POSTING_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.PAYMENT_POSTING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.PAYMENT_POSTING_FAILED)
                .and()
                .withExternal()
                    .source(WorkflowState.ACCOUNTING)
                    .target(WorkflowState.NOTIFICATION)
                    .event(WorkflowEvent.ACCOUNTING_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.ACCOUNTING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.ACCOUNTING_FAILED)
                .and()
                .withExternal()
                    .source(WorkflowState.NOTIFICATION)
                    .target(WorkflowState.PAYMENT_COMPLETED)
                    .event(WorkflowEvent.NOTIFICATION_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.NOTIFICATION)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.NOTIFICATION_FAILED)
                .and()
                .withExternal()
                    .source(WorkflowState.REQUEST_VALIDATION)
                    .target(WorkflowState.REQUEST_VALIDATION)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.FIRCO_SCREENING)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.PAYMENT_POSTING)
                    .target(WorkflowState.PAYMENT_POSTING)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.ACCOUNTING)
                    .target(WorkflowState.ACCOUNTING)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.NOTIFICATION)
                    .target(WorkflowState.NOTIFICATION)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.REQUEST_VALIDATION)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.PAYMENT_POSTING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.ACCOUNTING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.NOTIFICATION)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL);
    }
}
