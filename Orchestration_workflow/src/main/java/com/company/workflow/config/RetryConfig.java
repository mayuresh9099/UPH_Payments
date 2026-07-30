package com.company.workflow.config;

import com.company.workflow.retry.Sleeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

    @Bean
    Sleeper sleeper() {
        return duration -> Thread.sleep(duration.toMillis());
    }
}
