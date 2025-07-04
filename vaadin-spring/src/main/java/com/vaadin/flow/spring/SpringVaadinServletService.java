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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinTaskExecutor;

/**
 * Spring application context aware Vaadin servlet service implementation.
 *
 * @author Vaadin Ltd
 */
public class SpringVaadinServletService extends VaadinServletService {

    private final transient ApplicationContext context;

    static final String SPRING_BOOT_WEBPROPERTIES_CLASS = "org.springframework.boot.autoconfigure.web.WebProperties";

    private Set<String> multipleExecutorCandidates;

    /**
     * Creates an instance connected to the given servlet and using the given
     * configuration with provided application {@code context}.
     *
     * @param servlet
     *            the servlet which receives requests
     * @param deploymentConfiguration
     *            the configuration to use
     * @param context
     *            the Spring application context
     */
    public SpringVaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration,
            ApplicationContext context) {
        super(servlet, deploymentConfiguration);
        this.context = context;
    }

    @Override
    protected Optional<Instantiator> loadInstantiators()
            throws ServiceException {
        Optional<Instantiator> spiInstantiator = super.loadInstantiators();
        List<Instantiator> springInstantiators = context
                .getBeansOfType(Instantiator.class).values().stream()
                .collect(Collectors.toList());
        if (spiInstantiator.isPresent() && !springInstantiators.isEmpty()) {
            throw new ServiceException(
                    "Cannot init VaadinService because there are multiple eligible "
                            + "instantiator implementations: Java SPI registered instantiator "
                            + spiInstantiator.get()
                            + " and Spring instantiator beans: "
                            + springInstantiators);
        }
        if (!spiInstantiator.isPresent() && springInstantiators.isEmpty()) {
            Instantiator defaultInstantiator = new SpringInstantiator(this,
                    context);
            return Optional.of(defaultInstantiator);
        }
        return spiInstantiator.isPresent() ? spiInstantiator
                : springInstantiators.stream().findFirst();
    }

    @Override
    protected Executor createDefaultExecutor() {
        Set<String> candidates = Arrays
                .stream(context.getBeanNamesForType(TaskExecutor.class))
                .collect(Collectors.toCollection(HashSet::new));

        // No executor beans defined, fallback to Vaadin's default
        if (candidates.isEmpty()) {
            return super.createDefaultExecutor();
        }

        // Check for @VaadinTaskExecutor annotated beans, filter for
        // TaskExecutors types, and warn if the annotated bean is of an
        // unexpected type.
        Set<String> annotatedBeans = new HashSet<>(Set.of(
                context.getBeanNamesForAnnotation(VaadinTaskExecutor.class)));
        Set<String> invalidAnnotatedTypes = annotatedBeans.stream()
                .filter(beanName -> !candidates.contains(beanName))
                .collect(Collectors.toSet());
        if (!invalidAnnotatedTypes.isEmpty()) {
            LoggerFactory.getLogger(SpringVaadinServletService.class.getName())
                    .warn("Found beans with @{} annotation but not of type {}: {}. "
                            + "Remove the annotation from the bean definition.",
                            VaadinTaskExecutor.class.getSimpleName(),
                            TaskExecutor.class.getSimpleName(),
                            invalidAnnotatedTypes);
            annotatedBeans.removeAll(invalidAnnotatedTypes);
        }

        // Retain only the Vaadin specific executors if they are defined
        if (candidates.contains(VaadinTaskExecutor.NAME)
                || !annotatedBeans.isEmpty()) {
            candidates.removeIf(name -> !annotatedBeans.contains(name)
                    && !name.equals(VaadinTaskExecutor.NAME));
        }

        if (candidates.size() > 1) {
            // Gives preference to regular executors over schedulers when both
            // types are present.
            Map<Boolean, List<String>> byType = candidates.stream()
                    .collect(Collectors.partitioningBy(name -> context
                            .isTypeMatch(name, TaskScheduler.class)));
            if (!byType.get(true).isEmpty() && !byType.get(false).isEmpty()) {
                // Remove TaskScheduler's from candidates list
                byType.get(true).forEach(candidates::remove);
            }
        }

        if (candidates.size() > 1) {
            // Remove Spring default executor to select an application defined
            // bean
            candidates.remove("applicationTaskExecutor");
        }
        if (candidates.size() > 1) {
            multipleExecutorCandidates = candidates;
        }
        return context.getBean(candidates.iterator().next(),
                TaskExecutor.class);
    }

    @Override
    public Executor getExecutor() {
        if (multipleExecutorCandidates != null) {
            String message = String.format(
                    "Multiple TaskExecutor beans found: %s. "
                            + "Please resolve this conflict by either: "
                            + "(1) Providing a single TaskExecutor bean, or "
                            + "(2) Marking the bean to use with Vaadin by: "
                            + "naming it '%s' (e.g. @Bean(\"%s\")), or "
                            + "applying the @%s qualifier annotation to the bean definition.",
                    multipleExecutorCandidates, VaadinTaskExecutor.NAME,
                    VaadinTaskExecutor.NAME,
                    VaadinTaskExecutor.class.getSimpleName());
            throw new IllegalStateException(message);
        }
        return super.getExecutor();
    }

    @Override
    public void init() throws ServiceException {
        super.init();
        Map<String, UIInitListener> uiInitListeners = context
                .getBeansOfType(UIInitListener.class);
        uiInitListeners.values().forEach(this::addUIInitListener);
    }

    // This method should be removed when the deprecated class
    // SpringVaadinSession is removed
    @Override
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new SpringVaadinSession(this);
    }

    @Override
    public URL getStaticResource(String path) {
        URL resource = super.getStaticResource(path);
        if (resource == null) {
            resource = getResourceURL(path);
        }
        return resource;
    }

    private URL getResourceURL(String path) {
        if (!isSpringBootConfigured()) {
            return null;
        }
        for (String prefix : context.getBean(
                org.springframework.boot.autoconfigure.web.WebProperties.class)
                .getResources().getStaticLocations()) {
            Resource resource = context.getResource(getFullPath(path, prefix));
            if (resource != null && resource.exists()) {
                try {
                    URI uri = resource.getURI();
                    if (uri.isOpaque() && resource.isFile()) {
                        // Prevents 'URI is not hierarchical' error
                        return resource.getFile().getAbsoluteFile().toURI()
                                .toURL();
                    }
                    return resource.getURL();
                } catch (IOException e) {
                    // NO-OP file was not found.
                }
            }
        }
        return null;
    }

    private String getFullPath(String path, String prefix) {
        if (prefix.endsWith("/") && path.startsWith("/")) {
            return prefix + path.substring(1);
        }
        return prefix + path;
    }

    /**
     * Checks if the spring boot resources class is available without causing
     * ClassNotFound or similar exceptions in plain Spring.
     */
    private boolean isSpringBootConfigured() {
        Class<?> resourcesClass = resolveClass(SPRING_BOOT_WEBPROPERTIES_CLASS);
        if (resourcesClass != null) {
            return context.getBeanNamesForType(resourcesClass).length != 0;
        }
        return false;
    }

    private static Class<?> resolveClass(String clazzName) {
        try {
            return Class.forName(clazzName, false,
                    SpringVaadinServletService.class.getClassLoader());
        } catch (LinkageError | ClassNotFoundException e) {
            return null;
        }
    }

}
