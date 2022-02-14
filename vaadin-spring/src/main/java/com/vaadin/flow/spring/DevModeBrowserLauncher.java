/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.io.Serializable;

import javax.servlet.ServletContext;

import com.vaadin.base.devserver.util.BrowserLauncher;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Utilities for launching a browser when running in development mode.
 */
public class DevModeBrowserLauncher
        implements SpringApplicationRunListener, Serializable {

    private static final String LAUNCH_TRACKER = "LaunchUtil.hasLaunched";
    private static final String LAUNCHED_VALUE = "yes";

    public DevModeBrowserLauncher(SpringApplication application,
            String[] arguments) {
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        try {
            VaadinConfigurationProperties properties = context
                    .getBean(VaadinConfigurationProperties.class);

            if (properties.isLaunchBrowser()) {
                launchBrowserInDevelopmentMode(context);
            }
        } catch (Exception e) {
            getLogger().debug("Failed to launch browser", e);
        }
    }

    /**
     * Launch the default browser and open the given application base URL if
     * running in development mode.
     *
     * Does nothing if the application is running in production mode.
     * 
     * @param applicationContext
     *            the application context
     */
    private void launchBrowserInDevelopmentMode(ApplicationContext appContext) {
        if (isLaunched()) {
            // Only launch browser on startup, not on reload
            return;
        }
        if (!(appContext instanceof GenericWebApplicationContext)) {
            getLogger().warn(
                    "Unable to determine production mode for an Spring Boot application context of type "
                            + appContext.getClass().getName());
            return;
        }
        GenericWebApplicationContext webAppContext = (GenericWebApplicationContext) appContext;
        if (!DevModeBrowserLauncher.isProductionMode(webAppContext)) {
            String location = getUrl(webAppContext);
            String outputOnFailure = "Application started at " + location;
            try {
                BrowserLauncher.launch(location, outputOnFailure);
                setLaunched();
            } catch (Exception | NoClassDefFoundError e) { // NOSONAR
                // NoClassDefFoundError happens if vaadin-dev-server is not on
                // the classpath
                getLogger().info(outputOnFailure);
            }
        }
    }

    static String getUrl(GenericWebApplicationContext app) {
        String port = app.getEnvironment().getProperty("server.port");
        String host = "http://localhost:" + port;

        String path = "/";
        String vaadinServletMapping = app.getEnvironment()
                .getProperty("vaadin.urlMapping");
        ServletContext servletContext = app.getServletContext();
        if (servletContext != null) {
            String contextPath = servletContext.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                path = contextPath + "/";
            }
        }

        if (vaadinServletMapping != null && !vaadinServletMapping.isEmpty()) {
            if (vaadinServletMapping.startsWith("/")) {
                vaadinServletMapping = vaadinServletMapping.substring(1);
            }
            if (vaadinServletMapping.endsWith("*")) {
                vaadinServletMapping = vaadinServletMapping.substring(0,
                        vaadinServletMapping.length() - 1);

            }
            path += vaadinServletMapping;
        }

        return host + path;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeBrowserLauncher.class);
    }

    private static boolean isProductionMode(GenericWebApplicationContext app) {
        ServletContext servletContext = app.getServletContext();
        VaadinContext context = new VaadinServletContext(servletContext);
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration
                .get(context);
        return applicationConfiguration.isProductionMode();
    }

    private static boolean isLaunched() {
        return LAUNCHED_VALUE.equals(System.getProperty(LAUNCH_TRACKER));
    }

    private static void setLaunched() {
        System.setProperty(LAUNCH_TRACKER, LAUNCHED_VALUE);
    }

}
