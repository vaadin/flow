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

import jakarta.servlet.ServletContext;

import java.io.Serializable;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Utilities for launching a browser when running in development mode.
 */
public class DevModeBrowserLauncher
        implements SpringApplicationRunListener, Serializable {

    public DevModeBrowserLauncher(SpringApplication application,
            String[] arguments) {
    }

    @Override
    public void ready(ConfigurableApplicationContext context,
            Duration timeTaken) {
        try {
            VaadinConfigurationProperties properties = context
                    .getBean(VaadinConfigurationProperties.class);

            maybeLaunchBrowserInDevelopmentMode(context,
                    properties.isLaunchBrowser());
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
     * @param appContext
     *            the application context
     * @param launch
     *            true to launch the browser, false to only report the url
     */
    private void maybeLaunchBrowserInDevelopmentMode(
            ApplicationContext appContext, boolean launch) {
        if (!(appContext instanceof GenericWebApplicationContext)) {
            getLogger().warn(
                    "Unable to determine production mode for an Spring Boot application context of type "
                            + appContext.getClass().getName());
            return;
        }
        GenericWebApplicationContext webAppContext = (GenericWebApplicationContext) appContext;

        ServletContext servletContext = webAppContext.getServletContext();
        VaadinContext vaadinContext = new VaadinServletContext(servletContext);
        Lookup lookup = vaadinContext.getAttribute(Lookup.class);
        DevModeHandlerManager devModeHandlerManager = lookup
                .lookup(DevModeHandlerManager.class);
        if (devModeHandlerManager != null) {
            String url = getUrl(webAppContext);
            devModeHandlerManager.setApplicationUrl(url);
            if (launch) {
                devModeHandlerManager.launchBrowserInDevelopmentMode(url);
            }
        }
    }

    static String getUrl(GenericWebApplicationContext app) {
        String port = app.getEnvironment().getProperty("server.port");
        if (port == null) {
            port = "8080";
        }
        String sslEnabled = app.getEnvironment()
                .getProperty("server.ssl.enabled");
        String proto;
        if (sslEnabled != null && sslEnabled.equals("true")) {
            proto = "https";
        } else {
            proto = "http";
        }
        String host = proto + "://localhost:" + port;

        String path = "/";
        String vaadinServletMapping = RootMappedCondition
                .getUrlMapping(app.getEnvironment());

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

}
