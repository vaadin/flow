/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.spring;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.vaadin.router.Route;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.startup.AbstractRouteRegistryInitializer;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;

/**
 * Servlet context initializer for Spring Boot Application.
 * <p>
 * If Java application is used to run Spring Boot then it doesn't scan for
 * {@link Route} annotations in the classpath (it skips
 * {@link ServletContainerInitializer}s). This class enables this scanning via
 * Spring so that router may be used in the same way as in deployable WAR file.
 *
 * @see ServletContainerInitializer
 * @see RouteRegistry
 *
 * @author Vaadin Ltd
 *
 */
public class RouterRegistryServletContextInitializer extends
        AbstractRouteRegistryInitializer implements ServletContextInitializer {

    private ApplicationContext appContext;

    private ServletContextListener listener = new ServletContextListener() {

        @Override
        public void contextInitialized(ServletContextEvent event) {
            RouteRegistry registry = RouteRegistry
                    .getInstance(event.getServletContext());
            if (registry.isInitialized()) {
                return;
            }

            try {
                Set<Class<? extends Component>> navigationTargets = findNavigationTargets();

                registry.setNavigationTargets(navigationTargets);
            } catch (InvalidRouteConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            // no need care about destroyed context
        }

    };

    /**
     * Creates a new {@link ServletContextInitializer} instance with application
     * {@code context} provided.
     *
     * @param context
     *            the application context
     */
    public RouterRegistryServletContextInitializer(ApplicationContext context) {
        appContext = context;
    }

    @Override
    public void onStartup(ServletContext servletContext)
            throws ServletException {

        RouteRegistry registry = RouteRegistry.getInstance(servletContext);
        // If the registry is already initialized then RouteRegistryInitializer
        // has done its job already, skip the custom routes search
        if (registry.isInitialized()) {
            return;
        }
        /*
         * Don't rely on RouteRegistry.isInitialized() negative return value
         * here because it's not known whether RouteRegistryInitializer has been
         * executed already or not (the order is undefined). Postpone this to
         * the end of context initialization cycle. At this point RouteRegistry
         * is either initialized or it's not initialized because an
         * RouteRegistryInitializer has not been executed (end never will).
         */
        servletContext.addListener(listener);
    }

    private Set<Class<? extends Component>> findNavigationTargets() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        scanner.setResourceLoader(appContext);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        return validateRouteClasses(getRoutePackages().stream()
                .map(scanner::findCandidateComponents)
                .flatMap(set -> set.stream()).map(this::getNavigationTarget));
    }

    private Class<?> getNavigationTarget(BeanDefinition beanDefinition) {
        AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinition;
        Class<?> beanClass;
        if (definition.hasBeanClass()) {
            beanClass = definition.getBeanClass();
        } else {
            try {
                beanClass = definition
                        .resolveBeanClass(getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return beanClass;
    }

    private List<String> getRoutePackages() {
        if (AutoConfigurationPackages.has(appContext)) {
            return AutoConfigurationPackages.get(appContext);
        }
        return Collections.emptyList();
    }

}
