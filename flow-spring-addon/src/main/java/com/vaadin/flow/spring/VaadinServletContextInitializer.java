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
package com.vaadin.flow.spring;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.startup.AbstractAnnotationValidator;
import com.vaadin.flow.server.startup.AbstractCustomElementRegistryInitializer;
import com.vaadin.flow.server.startup.AbstractRouteRegistryInitializer;
import com.vaadin.flow.server.startup.AnnotationValidator;
import com.vaadin.flow.server.startup.CustomElementRegistry;
import com.vaadin.flow.server.startup.RouteRegistry;
import com.vaadin.flow.server.startup.ServletVerifier;
import com.vaadin.flow.spring.VaadinScanPackagesRegistrar.VaadinScanPackages;

/**
 * Servlet context initializer for Spring Boot Application.
 * <p>
 * If Java application is used to run Spring Boot then it doesn't run registered
 * {@link ServletContainerInitializer}s (e.g. to scan for {@link Route}
 * annotations in the classpath). This class enables this scanning via Spring so
 * that the functionality which relies on {@link ServletContainerInitializer}
 * works in the same way as in deployable WAR file.
 *
 * @see ServletContainerInitializer
 * @see RouteRegistry
 *
 * @author Vaadin Ltd
 *
 */
public class VaadinServletContextInitializer
        implements ServletContextInitializer {

    private ApplicationContext appContext;

    private class RouteServletContextListener extends
            AbstractRouteRegistryInitializer implements ServletContextListener {

        @SuppressWarnings("unchecked")
        @Override
        public void contextInitialized(ServletContextEvent event) {
            RouteRegistry registry = RouteRegistry
                    .getInstance(event.getServletContext());
            if (registry.navigationTargetsInitialized()) {
                return;
            }

            try {
                Set<Class<? extends Component>> navigationTargets = validateRouteClasses(
                        findByAnnotation(getRoutePackages(), Route.class,
                                RouteAlias.class));

                registry.setNavigationTargets(navigationTargets);
            } catch (InvalidRouteConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            // no need to do anything
        }

    }

    private class CustomElementServletContextListener
            extends AbstractCustomElementRegistryInitializer
            implements ServletContextListener {

        @Override
        @SuppressWarnings("unchecked")
        public void contextInitialized(ServletContextEvent event) {
            CustomElementRegistry registry = CustomElementRegistry
                    .getInstance();
            if (registry.isInitialized()) {
                return;
            }

            registry.setCustomElements(filterCustomElements(
                    findByAnnotation(getCustomElementPackages(), Tag.class)));
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            // no need to do anything
        }

    }

    private class ErrorParameterServletContextListener
            implements ServletContextListener {

        @Override
        @SuppressWarnings("unchecked")
        public void contextInitialized(ServletContextEvent event) {
            RouteRegistry registry = RouteRegistry
                    .getInstance(event.getServletContext());
            if (registry.errorNavigationTargetsInitialized()) {
                return;
            }

            Stream<Class<? extends Component>> hasErrorComponents = findBySuperType(
                    getErrorParameterPackages(), HasErrorParameter.class)
                            .filter(Component.class::isAssignableFrom)
                            .map(clazz -> (Class<? extends Component>) clazz);
            registry.setErrorNavigationTargets(
                    hasErrorComponents.collect(Collectors.toSet()));
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            // no need to do anything
        }

    }

    private class AnnotationValidatorServletContextListener extends
            AbstractAnnotationValidator implements ServletContextListener {

        @Override
        @SuppressWarnings("unchecked")
        public void contextInitialized(ServletContextEvent event) {
            Stream<Class<? extends Annotation>> annotations = getAnnotations()
                    .stream()
                    .map(annotation -> (Class<? extends Annotation>) annotation);
            validateClasses(findByAnnotation(getVerifiableAnnotationPackages(),
                    annotations).collect(Collectors.toList()));
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            // no need to do anything
        }

        @Override
        protected List<Class<?>> getAnnotations() {
            return Arrays.asList(AnnotationValidator.class
                    .getAnnotation(HandlesTypes.class).value());
        }

    }

    /**
     * Creates a new {@link ServletContextInitializer} instance with application
     * {@code context} provided.
     *
     * @param context
     *            the application context
     */
    public VaadinServletContextInitializer(ApplicationContext context) {
        appContext = context;
    }

    @Override
    public void onStartup(ServletContext servletContext)
            throws ServletException {
        // Verify servlet version also for SpringBoot.
        ServletVerifier.verifyServletVersion();

        RouteRegistry registry = RouteRegistry.getInstance(servletContext);
        // If the registry is already initialized then RouteRegistryInitializer
        // has done its job already, skip the custom routes search
        if (!registry.navigationTargetsInitialized()) {
            /*
             * Don't rely on RouteRegistry.isInitialized() negative return value
             * here because it's not known whether RouteRegistryInitializer has
             * been executed already or not (the order is undefined). Postpone
             * this to the end of context initialization cycle. At this point
             * RouteRegistry is either initialized or it's not initialized
             * because an RouteRegistryInitializer has not been executed (end
             * never will).
             */
            servletContext.addListener(new RouteServletContextListener());
        }

        if (!registry.errorNavigationTargetsInitialized()) {
            // Same thing: don't rely on hasNavigationTargets() negative return
            // value
            servletContext
                    .addListener(new ErrorParameterServletContextListener());
        }

        CustomElementRegistry elementRegistry = CustomElementRegistry
                .getInstance();
        if (!elementRegistry.isInitialized()) {
            // Same thing: don't rely on isInitialized() negative return value
            servletContext
                    .addListener(new CustomElementServletContextListener());
        }

        servletContext
                .addListener(new AnnotationValidatorServletContextListener());
    }

    @SuppressWarnings("unchecked")
    private Stream<Class<?>> findByAnnotation(Collection<String> packages,
            Class<? extends Annotation>... annotations) {
        return findByAnnotation(packages, Stream.of(annotations));
    }

    @SuppressWarnings("unchecked")
    private Stream<Class<?>> findByAnnotation(Collection<String> packages,
            Stream<Class<? extends Annotation>> annotations) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        scanner.setResourceLoader(appContext);
        annotations.forEach(annotation -> scanner
                .addIncludeFilter(new AnnotationTypeFilter(annotation)));

        return packages.stream().map(scanner::findCandidateComponents)
                .flatMap(set -> set.stream()).map(this::getBeanClass);
    }

    private Stream<Class<?>> findBySuperType(Collection<String> packages,
            Class<?> type) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        scanner.setResourceLoader(appContext);
        scanner.addIncludeFilter(new AssignableTypeFilter(type));

        return packages.stream().map(scanner::findCandidateComponents)
                .flatMap(set -> set.stream()).map(this::getBeanClass);
    }

    private Class<?> getBeanClass(BeanDefinition beanDefinition) {
        AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinition;
        Class<?> beanClass;
        if (definition.hasBeanClass()) {
            beanClass = definition.getBeanClass();
        } else {
            try {
                beanClass = definition
                        .resolveBeanClass(appContext.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return beanClass;
    }

    private Collection<String> getRoutePackages() {
        return getDefaultPackages();
    }

    private Collection<String> getCustomElementPackages() {
        return getDefaultPackages();
    }

    private Collection<String> getVerifiableAnnotationPackages() {
        return getDefaultPackages();
    }

    private Collection<String> getErrorParameterPackages() {
        return Stream
                .concat(Stream
                        .of(HasErrorParameter.class.getPackage().getName()),
                        getDefaultPackages().stream())
                .collect(Collectors.toSet());
    }

    private List<String> getDefaultPackages() {
        List<String> packagesList = Collections.emptyList();
        if (appContext
                .getBeanNamesForType(VaadinScanPackages.class).length > 0) {
            VaadinScanPackages packages = appContext
                    .getBean(VaadinScanPackages.class);
            packagesList = packages.getScanPackages();

        }
        if (!packagesList.isEmpty()) {
            LoggerFactory.getLogger(VaadinServletContextInitializer.class)
                    .trace("Using explicitly configured packages for scan Vaadin types at startup {}",
                            packagesList);
        } else if (AutoConfigurationPackages.has(appContext)) {
            packagesList = AutoConfigurationPackages.get(appContext);
        }
        return packagesList;
    }

}
