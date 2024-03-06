/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.mvc;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.vaadin.flow.spring.VaadinMVCWebAppInitializer;
import com.vaadin.flow.spring.test.TestConfiguration;

/**
 * The entry point for Spring MVC.
 *
 * @author Vaadin Ltd
 *
 */
public class TestWebAppInitializer extends VaadinMVCWebAppInitializer {

    @Autowired
    private ConfigurableEnvironment env;

    @Override
    protected Collection<Class<?>> getConfigurationClasses() {
        return Collections.singletonList(TestConfiguration.class);
    }

    @Override
    protected void registerConfiguration(
            AnnotationConfigWebApplicationContext context) {
        context.getEnvironment().setActiveProfiles("enabled");
        super.registerConfiguration(context);
    }
}
