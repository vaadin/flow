/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DeploymentConfigurationCreator;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Context listener that automatically registers a Vaadin servlet. The servlet
 * is only registered if all of the following conditions apply:
 * <ul>
 * <li>At least one class annotated with {@link Route @Route} is found on the
 * classpath
 * <li>No servlet is registered for <code>/*</code>
 * <li>No Vaadin servlet is registered
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServletDeployer implements ServletContextListener {

    private static final String SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE = "Skipping automatic servlet registration because";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        createAppServlet(context);
        if (hasDevelopmentModeServlet(context)) {
            createServletIfNotExists(context, "frontendFilesServlet",
                    "/frontend/*");
        }
    }

    private boolean hasDevelopmentModeServlet(ServletContext context) {
        for (ServletRegistration registration : context
                .getServletRegistrations().values()) {
            Optional<Class<?>> servletClass = loadClass(
                    context.getClassLoader(), registration.getClassName());
            if (!servletClass.map(this::isVaadinServlet).orElse(false)) {
                continue;
            }

            DeploymentConfiguration configuration;
            try {
                configuration = DeploymentConfigurationCreator
                        .createDeploymentConfiguration(servletClass.get(),
                                getServletConfig(context, registration));
            } catch (ServletException e) {
                throw new IllegalStateException(e);
            }
            if (!configuration.isProductionMode()) {
                return true;
            }
        }

        return false;
    }

    private ServletConfig getServletConfig(ServletContext context,
            ServletRegistration registration) {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return registration.getName();
            }

            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public String getInitParameter(String name) {
                return registration.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections
                        .enumeration(registration.getInitParameters().keySet());
            }
        };
    }

    private void createAppServlet(ServletContext servletContext) {
        if (!RouteRegistry.getInstance(servletContext).hasNavigationTargets()) {
            getLogger().info(
                    "{} there are no navigation targets annotated with @Route",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE);
            return;
        }

        ServletRegistration vaadinServlet = findVaadinServlet(servletContext);
        if (vaadinServlet != null) {
            getLogger().info(
                    "{} there is already a Vaadin servlet with the name {}",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE,
                    vaadinServlet.getName());
            return;
        }

        createServletIfNotExists(servletContext, "/*", getClass().getName());
    }

    private void createServletIfNotExists(ServletContext context, String name,
            String path) {
        ServletRegistration existingServlet = findServletByPathPart(context,
                path);
        if (existingServlet != null) {
            getLogger().info(
                    "{} there is already a {} servlet with the name {} for path {} given",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE,
                    existingServlet, existingServlet.getName(), path);
            return;
        }

        ServletRegistration.Dynamic registration = context.addServlet(name,
                VaadinServlet.class);
        if (registration == null) {
            // Not expected to ever happen
            getLogger().info("{} there is already a servlet with the name {}",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE, name);
            return;
        }

        getLogger().info(
                "Automatically deploying Vaadin servlet with name {} to {}",
                name, path);

        registration.setAsyncSupported(true);
        registration.addMapping(path);
    }

    private ServletRegistration findServletByPathPart(ServletContext context,
            String path) {
        return context.getServletRegistrations().values().stream().filter(
                registration -> registration.getMappings().contains(path))
                .findAny().orElse(null);
    }

    private ServletRegistration findVaadinServlet(ServletContext context) {
        return context.getServletRegistrations().values().stream()
                .filter(registration -> isVaadinServlet(
                        context.getClassLoader(), registration.getClassName()))
                .findAny().orElse(null);
    }

    private boolean isVaadinServlet(ClassLoader classLoader, String className) {
        return loadClass(classLoader, className)
                .map(VaadinServlet.class::isAssignableFrom).orElse(false);
    }

    private boolean isVaadinServlet(Class<?> servletClass) {
        return VaadinServlet.class.isAssignableFrom(servletClass);
    }

    private Optional<Class<?>> loadClass(ClassLoader classLoader,
            String className) {
        try {
            return Optional.of(classLoader.loadClass(className));
        } catch (ClassNotFoundException e) {
            getLogger().warn(
                    "Failed to load class {}, ignoring it when deploying Vaadin servlets",
                    className, e);
            return Optional.empty();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nothing to do
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ServletDeployer.class.getName());
    }
}
