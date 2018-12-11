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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DeploymentConfigurationFactory;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfiguration;

/**
 * Context listener that automatically registers Vaadin servlets.
 *
 * The servlets registered are:
 * <ul>
 * <li>Vaadin application servlet, mapped to '/*'<br>
 * The servlet won't be registered, if any {@link VaadinServlet} is registered
 * already or if there are no classes annotated with {@link Route}
 * annotation.</li>
 * <li>Frontend files servlet, mapped to '/frontend/*' <br>
 * The servlet is registered when the application is started in the development
 * mode or has
 * {@link com.vaadin.flow.server.Constants#USE_ORIGINAL_FRONTEND_RESOURCES}
 * parameter set to {@code true}.</li>
 * </ul>
 *
 * In addition to the rules above, a servlet won't be registered, if any servlet
 * had been mapped to the same path already or if
 * {@link com.vaadin.flow.server.Constants#DISABLE_AUTOMATIC_SERVLET_REGISTRATION}
 * system property is set to {@code true}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @see VaadinServletConfiguration#disableAutomaticServletRegistration()
 */
public class ServletDeployer implements ServletContextListener {
    private static final String SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE = "Skipping automatic servlet registration because";

    private static class StubServletConfig
            implements ServletConfig {
        private final ServletContext context;
        private final ServletRegistration registration;

        private StubServletConfig(ServletContext context,
                ServletRegistration registration) {
            this.context = context;
            this.registration = registration;
        }

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
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Collection<DeploymentConfiguration> servletConfigurations = getServletConfigurations(
                context);

        boolean enableServlets = true;
        boolean hasDevelopmentMode = servletConfigurations.isEmpty();
        for (DeploymentConfiguration configuration : servletConfigurations) {
            enableServlets = enableServlets
                    && !configuration.disableAutomaticServletRegistration();
            hasDevelopmentMode = hasDevelopmentMode
                    || !configuration.useCompiledFrontendResources();
        }

        if (enableServlets) {
            createAppServlet(context);
            if (hasDevelopmentMode) {
                createServletIfNotExists(context, "frontendFilesServlet",
                        "/frontend/*");
            }
        }
    }

    private Collection<DeploymentConfiguration> getServletConfigurations(
            ServletContext context) {
        Collection<? extends ServletRegistration> registrations = context
                .getServletRegistrations().values();
        Collection<DeploymentConfiguration> result = new ArrayList<>(
                registrations.size());
        for (ServletRegistration registration : registrations) {
            loadClass(context.getClassLoader(), registration.getClassName())
                    .ifPresent(
                            servletClass -> result
                                    .add(createDeploymentConfiguration(
                                            new StubServletConfig(context,
                                                    registration),
                                            servletClass)));
        }
        return result;
    }

    private DeploymentConfiguration createDeploymentConfiguration(
            ServletConfig servletConfig, Class<?> servletClass) {
        try {
            return DeploymentConfigurationFactory
                    .createPropertyDeploymentConfiguration(servletClass, servletConfig);
        } catch (ServletException e) {
            throw new IllegalStateException(String.format(
                    "Failed to get deployment configuration data for servlet with name '%s' and class '%s'",
                    servletConfig.getServletName(), servletClass), e);
        }
    }

    private void createAppServlet(ServletContext context) {
        if (!ApplicationRouteRegistry.getInstance(context).hasNavigationTargets()) {
            getLogger().info(
                    "{} there are no navigation targets registered to the registry",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE);
            return;
        }

        ServletRegistration vaadinServlet = findVaadinServlet(context);
        if (vaadinServlet != null) {
            getLogger().info(
                    "{} there is already a Vaadin servlet with the name {}",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE,
                    vaadinServlet.getName());
            return;
        }

        createServletIfNotExists(context, getClass().getName(), "/*");
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
