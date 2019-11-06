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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
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
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DeploymentConfigurationFactory;
import com.vaadin.flow.server.FrontendVaadinServlet;
import com.vaadin.flow.server.VaadinConfigurationException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfig;
import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

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
 * <li>Static files servlet, mapped to '/VAADIN/static' responsible to resolve
 * files placed in the '[webcontext]/VAADIN/static' folder or in the
 * '[classpath]/META-INF/static' location. It prevents sensible files like
 * 'stats.json' and 'flow-build-info.json' to be served. It manages cache
 * headers based on the '.cache.' and '.nocache.' fragment in the file
 * name.</li>
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

    private enum VaadinServletCreation {
        NO_CREATION, SERVLET_EXISTS, SERVLET_CREATED;
    }

    /**
     * Default ServletConfig implementation.
     */
    public static class StubServletConfig implements ServletConfig {
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
            try {
                ServletConfig servletConfig = new StubServletConfig(context,
                        registration);
                return DeploymentConfigurationFactory
                        .createPropertyDeploymentConfiguration(servletClass,
                                new VaadinServletConfig(servletConfig));
            } catch (VaadinConfigurationException e) {
                throw new IllegalStateException(String.format(
                        "Failed to get deployment configuration data for servlet with name '%s' and class '%s'",
                        registration.getName(), servletClass), e);
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Collection<DeploymentConfiguration> servletConfigurations = getServletConfigurations(
                context);

        boolean enableServlets = true;
        boolean hasDevelopmentMode = servletConfigurations.isEmpty();
        boolean isCompatibilityMode = false;
        for (DeploymentConfiguration configuration : servletConfigurations) {
            enableServlets = enableServlets
                    && !configuration.disableAutomaticServletRegistration();
            boolean devMode = !configuration.useCompiledFrontendResources();
            hasDevelopmentMode = hasDevelopmentMode || devMode;
            if (devMode) {
                isCompatibilityMode = isCompatibilityMode
                        || configuration.isCompatibilityMode();
            }
        }

        /*
         * The default servlet is created using root mapping, in that case no
         * need to register extra servlet. We should register frontend servlet
         * only if there is a registered servlet.
         *
         * Also we don't need a frontend servlet at all in non compatibility
         * mode.
         */
        if (enableServlets
                && createAppServlet(
                        context) == VaadinServletCreation.SERVLET_EXISTS
                && hasDevelopmentMode && isCompatibilityMode) {
            createServletIfNotExists(context, "frontendFilesServlet",
                    FrontendVaadinServlet.class, "/frontend/*",
                    Collections.singletonMap(
                            Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                            Boolean.TRUE.toString()));
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
                    .ifPresent(servletClass -> result.add(
                            StubServletConfig.createDeploymentConfiguration(
                                    context, registration, servletClass)));
        }
        return result;
    }

    private VaadinServletCreation createAppServlet(ServletContext servletContext) {
        VaadinServletContext context = new VaadinServletContext(servletContext);
        boolean createServlet = ApplicationRouteRegistry.getInstance(context)
                .hasNavigationTargets();

        createServlet = createServlet || WebComponentConfigurationRegistry
                .getInstance(context).hasConfigurations();

        if (!createServlet) {
            getLogger().info(
                    "{} there are no navigation targets registered to the "
                            + "route registry and there are no web component exporters",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE);
            return VaadinServletCreation.NO_CREATION;
        }

        ServletRegistration vaadinServlet = findVaadinServlet(servletContext);
        if (vaadinServlet != null) {
            getLogger().info(
                    "{} there is already a Vaadin servlet with the name {}",
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
            getLogger().info(
                    "{} there is already a {} servlet with the name {} for path {} given",
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
            getLogger().info("{} there is already a servlet with the name {}",
                    SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE, name);
            return VaadinServletCreation.NO_CREATION;
        }

        getLogger().info(
                "Automatically deploying Vaadin servlet with name {} to {}",
                name, path);

        registration.setAsyncSupported(true);
        registration.addMapping(path);
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
