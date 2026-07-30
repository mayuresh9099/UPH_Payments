package com.company.workflow.config;

import com.company.workflow.action.MockActionProperties;
import java.util.random.RandomGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General application bean configuration.
 */
@Configuration
@EnableConfigurationProperties(MockActionProperties.class)
public class AppConfig {

    /**
     * Provides a default pseudo-random number generator for mock action implementations.
     *
     * @return the default {@link RandomGenerator}
     */
    @Bean
    RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }
}
