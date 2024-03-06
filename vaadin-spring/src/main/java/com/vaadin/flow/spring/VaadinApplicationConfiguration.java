/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.spring.SpringLookupInitializer.SpringApplicationContextInit;

/**
 * Vaadin Application Spring configuration.
 * <p>
 * Registers a default {@link ApplicationConfigurationFactory} for Vaadin web
 * application if there is no developer provided factory available.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Configuration
public class VaadinApplicationConfiguration {

    /**
     * Creates a default {@link ApplicationConfigurationFactory} bean in case
     * there is no developer provided bean.
     *
     * @return the default application configuration factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationConfigurationFactory defaultApplicationConfigurationFactory() {
        return new SpringApplicationConfigurationFactory();
    }

    /**
     * Creates an application context initializer for lookup initializer
     * {@link SpringLookupInitializer}.
     *
     * @return an application context initializer
     */
    @Bean
    public ApplicationContextAware vaadinApplicationContextInitializer() {
        return new SpringApplicationContextInit();
    }
}
