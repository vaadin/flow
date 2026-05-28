/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.micrometer.tests;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import com.vaadin.flow.micrometer.VaadinMetricsConfig;
import com.vaadin.flow.micrometer.VaadinMicrometer;

/**
 * Boots vaadin-micrometer at servlet-context startup so the SPI-loaded
 * {@code MetricsServiceInitListener} can pick up the registry when Vaadin's
 * service initializes.
 */
@WebListener
public class MicrometerSetup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        VaadinMicrometer.install(MicrometerRegistry.INSTANCE,
                VaadinMetricsConfig.defaults());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        VaadinMicrometer.uninstall();
    }
}
