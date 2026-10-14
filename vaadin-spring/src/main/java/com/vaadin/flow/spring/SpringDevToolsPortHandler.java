/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import com.vaadin.flow.internal.NetworkUtil;

/**
 * Sets Spring Boot dev tools to run on a free random port if the default port
 * (35729) is in use.
 */
public class SpringDevToolsPortHandler implements EnvironmentPostProcessor {

    private static final String SPRING_DEVTOOLS_LIVERELOAD_PORT = "spring.devtools.livereload.port";
    private static final int DEFAULT_PORT = 35729;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        if (environment.getProperty(SPRING_DEVTOOLS_LIVERELOAD_PORT) == null) {
            int reloadPort = DEFAULT_PORT;
            if (!NetworkUtil.isFreePort(reloadPort)) {
                reloadPort = NetworkUtil.getFreePort();
            }

            // We must set a system property and not a Spring Boot property so
            // it survives the server restart. We must also set the default port
            // so we do not try to find a new one after redeploy
            System.setProperty(SPRING_DEVTOOLS_LIVERELOAD_PORT,
                    reloadPort + "");
        }
    }

}
