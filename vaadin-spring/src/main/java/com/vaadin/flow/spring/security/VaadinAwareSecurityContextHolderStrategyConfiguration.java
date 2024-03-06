/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * Provides configuration of Vaadin aware {@link SecurityContextHolderStrategy}
 */
@Configuration
public class VaadinAwareSecurityContextHolderStrategyConfiguration {

    /**
     * Registers {@link SecurityContextHolderStrategy} bean.
     * <p>
     * Beans of this type will automatically be used by
     * {@link org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration}
     * to configure the current {@link SecurityContextHolderStrategy}.
     */
    @Bean(name = "VaadinSecurityContextHolderStrategy")
    @ConditionalOnMissingBean
    public VaadinAwareSecurityContextHolderStrategy securityContextHolderStrategy() {
        VaadinAwareSecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        // Use a security context holder that can find the context from Vaadin
        // specific classes
        SecurityContextHolder.setContextHolderStrategy(
                vaadinAwareSecurityContextHolderStrategy);
        return vaadinAwareSecurityContextHolderStrategy;
    }

}
