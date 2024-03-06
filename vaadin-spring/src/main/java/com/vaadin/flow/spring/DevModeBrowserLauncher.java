/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.io.Serializable;

import javax.servlet.ServletContext;

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
     * @param appContext
     *            the application context
     */
    private void launchBrowserInDevelopmentMode(ApplicationContext appContext) {
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
            devModeHandlerManager
                    .launchBrowserInDevelopmentMode(getUrl(webAppContext));
        }
    }

    static String getUrl(GenericWebApplicationContext app) {
        String port = app.getEnvironment().getProperty("server.port");
        String host = "http://localhost:" + port;

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
