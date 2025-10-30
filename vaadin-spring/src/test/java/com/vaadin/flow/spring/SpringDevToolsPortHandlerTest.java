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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringDevToolsPortHandlerTest {

    private SpringDevToolsPortHandler handler;
    private ConfigurableEnvironment environment;
    private SpringApplication application;

    @Before
    public void setUp() {
        handler = new SpringDevToolsPortHandler();
        environment = Mockito.mock(ConfigurableEnvironment.class);
        application = Mockito.mock(SpringApplication.class);
    }

    @Test
    public void liveReloadEnabled_portIsAssigned() {
        // Arrange: livereload enabled (default), no port set
        Mockito.when(environment.getProperty(
                "spring.devtools.livereload.enabled", Boolean.class, true))
                .thenReturn(true);
        Mockito.when(environment.getProperty("spring.devtools.livereload.port"))
                .thenReturn(null);

        // Act
        handler.postProcessEnvironment(environment, application);

        // Assert: port should be set via System.setProperty
        String portProperty = System
                .getProperty("spring.devtools.livereload.port");
        Assert.assertNotNull(
                "Port should be set when livereload is enabled",
                portProperty);

        // Verify it's a valid port number
        int port = Integer.parseInt(portProperty);
        Assert.assertTrue("Port should be positive", port > 0);
        Assert.assertTrue("Port should be in valid range", port <= 65535);

        // Clean up
        System.clearProperty("spring.devtools.livereload.port");
    }

    @Test
    public void liveReloadDisabled_portIsNotAssigned() {
        // Arrange: livereload explicitly disabled, no port set
        Mockito.when(environment.getProperty(
                "spring.devtools.livereload.enabled", Boolean.class, true))
                .thenReturn(false);
        Mockito.when(environment.getProperty("spring.devtools.livereload.port"))
                .thenReturn(null);

        // Clear any previously set system property
        System.clearProperty("spring.devtools.livereload.port");

        // Act
        handler.postProcessEnvironment(environment, application);

        // Assert: port should NOT be set
        String portProperty = System
                .getProperty("spring.devtools.livereload.port");
        Assert.assertNull(
                "Port should not be set when livereload is disabled",
                portProperty);
    }
}
