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
package com.vaadin.flow.server.startup;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DeploymentConfigurationFactory;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfig;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Context listener that automatically registers Vaadin servlets.
 * <p>
 * The servlets registered are:
 * <ul>
 * <li>Vaadin application servlet, mapped to '/*'<br>
 * The servlet won't be registered, if any {@link VaadinServlet} is registered
 * already or if there are no classes annotated with {@link Route}
 * annotation.</li>
 * <li>Static files servlet, mapped to '/VAADIN/static' responsible to resolve
 * files placed in the '[webcontext]/VAADIN/static' folder or in the
 * '[classpath]/META-INF/static' location. It prevents sensible files like
 * 'stats.json' and 'flow-build-info.json' to be served. It manages cache
 * headers based on the '.cache.' and '.nocache.' fragment in the file
 * name.</li>
 * </ul>
 * <p>
 * In addition to the rules above, a servlet won't be registered, if any servlet
 * had been mapped to the same path already or if
 * {@link InitParameters#DISABLE_AUTOMATIC_SERVLET_REGISTRATION} system property
 * is set to {@code true}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServletDeployer implements ServletContextListener {
    private static final String SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE = "Skipping automatic servlet registration because";

    private enum VaadinServletCreation {
        NO_CREATION, SERVLET_EXISTS, SERVLET_CREATED;
    }

    private String servletCreationMessage;

    /**
     * Default ServletConfig implementation.
     */
    private static class StubServletConfig implements ServletConfig {
        private final ServletContext context;
        private final ServletRegistration registration;

        /**
         * Constructor.
         *
         * @param context
         *            the ServletContext
         * @param registration
         *            the ServletRegistration for this ServletConfig instance
         */
        public StubServletConfig(ServletContext context,
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
            return registration.getInitParameters().get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections
                    .enumeration(registration.getInitParameters().keySet());
        }

        /**
         * Creates a DeploymentConfiguration.
         *
         * @param context
         *            the ServletContext
         * @param registration
         *            the ServletRegistration
         * @param servletClass
         *            the class to look for properties defined with annotations
         * @return a DeploymentConfiguration instance
         */
        public static DeploymentConfiguration createDeploymentConfiguration(
                ServletContext context, ServletRegistration registration,
                Class<?> servletClass) {
            ServletConfig servletConfig = new StubServletConfig(context,
                    registration);
            return new DeploymentConfigurationFactory()
                    .createPropertyDeploymentConfiguration(servletClass,
                            new VaadinServletConfig(servletConfig));
        }

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Collection<DeploymentConfiguration> servletConfigurations = getServletConfigurations(
                context);

        VaadinServletContext vaadinContext = new VaadinServletContext(context);
        ApplicationConfiguration config = ApplicationConfiguration
                .get(vaadinContext);
        boolean enableServlets = !config.disableAutomaticServletRegistration();
        boolean productionMode = false;
        for (DeploymentConfiguration configuration : servletConfigurations) {
            productionMode = productionMode || configuration.isProductionMode();
        }

        VaadinServletCreation servletCreation = enableServlets
                ? createAppServlet(context)
                : null;

        logServletCreation(servletCreation, productionMode);
    }

    private void logServletCreation(VaadinServletCreation servletCreation,
            boolean productionMode) {
        Logger logger = getLogger();

        if (servletCreation == null || productionMode) {
            // the servlet creation is explicitly disabled or production mode
            // activated - just info
            if (servletCreationMessage != null) {
                logger.info(servletCreationMessage);
            }
        } else if (servletCreation == VaadinServletCreation.NO_CREATION) {
            // debug mode and servlet not created for some reason - make it more
            // visible with warning
            logger.warn(servletCreationMessage);
        } else {
            logger.info(servletCreationMessage);
        }
    }

    private Collection<DeploymentConfiguration> getServletConfigurations(
            ServletContext context) {
        Collection<? extends ServletRegistration> registrations = context
                .getServletRegistrations().values();
        Collection<DeploymentConfiguration> result = new ArrayList<>(
                registrations.size());
        for (ServletRegistration registration : registrations) {
            if (registration.getClassName() != null) {
                loadClass(context.getClassLoader(), registration.getClassName())
                        .ifPresent(servletClass -> result.add(
                                StubServletConfig.createDeploymentConfiguration(
                                        context, registration, servletClass)));
            }
        }
        return result;
    }

    private VaadinServletCreation createAppServlet(
            ServletContext servletContext) {
        ServletRegistration vaadinServlet = findVaadinServlet(servletContext);
        if (vaadinServlet != null) {
            servletCreationMessage = String.format(
                    "%s there is already a Vaadin servlet with the name %s",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE,
                    vaadinServlet.getName());
            return VaadinServletCreation.SERVLET_EXISTS;
        }

        return createServletIfNotExists(servletContext, getClass().getName(),
                VaadinServlet.class, "/*");
    }

    private VaadinServletCreation createServletIfNotExists(
            ServletContext context, String name,
            Class<? extends Servlet> servletClass, String path) {
        return createServletIfNotExists(context, name, servletClass, path,
                null);
    }

    private VaadinServletCreation createServletIfNotExists(
            ServletContext context, String name,
            Class<? extends Servlet> servletClass, String path,
            Map<String, String> initParams) {
        ServletRegistration existingServlet = findServletByPathPart(context,
                path);
        if (existingServlet != null) {
            servletCreationMessage = String.format(
                    "%s there is already a %s servlet with the name %s for path %s given",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE,
                    existingServlet, existingServlet.getName(), path);
            return VaadinServletCreation.SERVLET_EXISTS;
        }

        ServletRegistration.Dynamic registration = context.addServlet(name,
                servletClass);
        if (initParams != null) {
            registration.setInitParameters(initParams);
        }
        if (registration == null) {
            // Not expected to ever happen
            servletCreationMessage = String.format(
                    "%s there is already a servlet with the name %s",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE, name);
            return VaadinServletCreation.NO_CREATION;
        }

        servletCreationMessage = String.format(
                "Automatically deploying Vaadin servlet with name %s to %s",
                name, path);

        registration.setAsyncSupported(true);
        registration.addMapping(path);
        registration.setLoadOnStartup(1);
        return VaadinServletCreation.SERVLET_CREATED;
    }

    private ServletRegistration findServletByPathPart(ServletContext context,
            String path) {
        return context.getServletRegistrations().values().stream().filter(
                registration -> registration.getMappings().contains(path))
                .findAny().orElse(null);
    }

    private ServletRegistration findVaadinServlet(ServletContext context) {
        return context.getServletRegistrations().values().stream()
                .filter(registration -> registration.getClassName() != null)
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
