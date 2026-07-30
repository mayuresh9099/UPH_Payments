package com.company.workflow.repository;

import com.company.workflow.entity.RetryConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetryConfigurationRepository extends JpaRepository<RetryConfiguration, String> {
}
