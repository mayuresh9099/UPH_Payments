package com.company.workflow.config;

import com.company.workflow.action.MockActionProperties;
import java.util.random.RandomGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MockActionProperties.class)
public class AppConfig {

    @Bean
    RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }
}
