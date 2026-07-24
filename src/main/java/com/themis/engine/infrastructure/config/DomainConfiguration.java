package com.themis.engine.infrastructure.config;

import com.themis.engine.domain.RuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates framework-managed instances of pure domain services.
 * Spring wiring belongs to infrastructure, not to the domain model.
 */
@Configuration
public class DomainConfiguration {

    @Bean
    public RuleEngine ruleEngine() {
        return new RuleEngine();
    }
}