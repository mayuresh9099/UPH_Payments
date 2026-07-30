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

/**
 * Spring State Machine configuration for the payment workflow.
 * <p>
 * Defines all states, terminal states, and transitions driven by {@link WorkflowEvent}s.
 * The state machine is created per workflow instance via {@link org.springframework.statemachine.config.StateMachineFactory}.
 * </p>
 */
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
                // PAYMENT_RECEIVED → VALIDATE_PAYMENT
                .withExternal()
                    .source(WorkflowState.PAYMENT_RECEIVED)
                    .target(WorkflowState.VALIDATE_PAYMENT)
                    .event(WorkflowEvent.START_PAYMENT)
                .and()
                // VALIDATE_PAYMENT transitions
                .withExternal()
                    .source(WorkflowState.VALIDATE_PAYMENT)
                    .target(WorkflowState.FIRCO_SCREENING)
                    .event(WorkflowEvent.VALIDATE_PAYMENT_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.VALIDATE_PAYMENT)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.VALIDATE_PAYMENT_FAILED)
                .and()
                // FIRCO_SCREENING transitions
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.FLEX_POSTING)
                    .event(WorkflowEvent.FIRCO_SCREENING_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FIRCO_SCREENING_FAILED)
                .and()
                // FLEX_POSTING transitions
                .withExternal()
                    .source(WorkflowState.FLEX_POSTING)
                    .target(WorkflowState.ACCOUNTING)
                    .event(WorkflowEvent.FLEX_POSTING_SUCCESS)
                .and()
                .withExternal()
                    .source(WorkflowState.FLEX_POSTING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FLEX_POSTING_FAILED)
                .and()
                // ACCOUNTING transitions
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
                // NOTIFICATION transitions
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
                // RETRY self-transitions for each retryable state
                .withExternal()
                    .source(WorkflowState.VALIDATE_PAYMENT)
                    .target(WorkflowState.VALIDATE_PAYMENT)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.FIRCO_SCREENING)
                    .event(WorkflowEvent.RETRY)
                .and()
                .withExternal()
                    .source(WorkflowState.FLEX_POSTING)
                    .target(WorkflowState.FLEX_POSTING)
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
                // FAIL transitions (retry exhausted)
                .withExternal()
                    .source(WorkflowState.VALIDATE_PAYMENT)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.FIRCO_SCREENING)
                    .target(WorkflowState.PAYMENT_FAILED)
                    .event(WorkflowEvent.FAIL)
                .and()
                .withExternal()
                    .source(WorkflowState.FLEX_POSTING)
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
