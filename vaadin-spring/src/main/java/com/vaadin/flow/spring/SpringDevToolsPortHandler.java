/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import com.vaadin.flow.internal.NetworkUtil;

/**
 * Sets Spring Boot dev tools to run on a free random port if the default port
 * (35729) is in use.
 */
public class SpringDevToolsPortHandler implements EnvironmentPostProcessor {

    private static final String SPRING_DEVTOOLS_LIVERELOAD_PORT = "spring.devtools.livereload.port";
    private static final String SPRING_DEVTOOLS_LIVERELOAD_ENABLED = "spring.devtools.livereload.enabled";
    private static final int DEFAULT_PORT = 35729;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        // Only set the port if livereload is enabled (defaults to true when
        // DevTools is present)
        boolean liveReloadEnabled = environment.getProperty(
                SPRING_DEVTOOLS_LIVERELOAD_ENABLED, Boolean.class, true);

        if (!liveReloadEnabled) {
            return;
        }

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
