/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.spring.scopes.VaadinRouteScope;
import com.vaadin.flow.spring.scopes.VaadinSessionScope;
import com.vaadin.flow.spring.scopes.VaadinUIScope;

/**
 * Vaadin scopes configuration.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
public class VaadinScopesConfig {

    /**
     * Creates a Vaadin session scope.
     *
     * @return the Vaadin session scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinSessionScope() {
        return new VaadinSessionScope();
    }

    /**
     * Creates a Vaadin UI scope.
     *
     * @return the Vaadin UI scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinUIScope() {
        return new VaadinUIScope();
    }

    /**
     * Creates a Vaadin route scope.
     *
     * @return the Vaadin route scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinRouteScope() {
        return new VaadinRouteScope();
    }
}
