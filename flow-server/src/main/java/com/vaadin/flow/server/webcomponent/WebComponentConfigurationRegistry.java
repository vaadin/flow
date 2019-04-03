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
package com.vaadin.flow.server.webcomponent;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.theme.Theme;

/**
 * Registry for storing available web component configuration implementations.
 *
 * @since
 */
@EmbeddedApplicationAnnotations({ Theme.class, Push.class })
public class WebComponentConfigurationRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private boolean exportersSet = false;
    private HashMap<String, WebComponentConfiguration<? extends Component>> configurationMap =
            new HashMap<>();

    private HashMap<Class<? extends Annotation>, Annotation> embeddedAppAnnotations;

    /**
     * Protected constructor for internal OSGi extensions.
     */
    protected WebComponentConfigurationRegistry() {
    }

    /**
     * Get a web component configuration for given custom element tag if one is
     * registered.
     *
     * @param tag
     *            custom element tag
     * @return Optional containing a web component configuration matching given
     * tag
     */
    public Optional<WebComponentConfiguration<? extends Component>> getConfiguration(
            String tag) {
        lock();
        try {
            return Optional.ofNullable(configurationMap.get(tag));
        } finally {
            unlock();
        }
    }

    /**
     * Get an unmodifiable set containing all registered web component
     * configurations for a specific {@link Component} type.
     *
     * @param componentClass
     *            type of the exported {@link Component}
     * @param <T>
     *            component
     * @return set of {@link WebComponentConfiguration} or an empty set.
     */
    public <T extends Component> Set<WebComponentConfiguration<T>> getConfigurationsByComponentType(
            Class<T> componentClass) {
        lock();
        try {
            return Collections.unmodifiableSet(configurationMap.values().stream()
                    .filter(config -> componentClass.equals(config.getComponentClass()))
                    .map(b -> (WebComponentConfiguration<T>) b)
                    .collect(Collectors.toSet()));
        } finally {
            unlock();
        }
    }

    /**
     * Internal method for updating registry.
     *
     * @param configurations
     *            set of web component configurations to register
     */
    protected void updateRegistry(
            Set<Class<? extends WebComponentExporter<? extends Component>>> configurations) {
        lock();
        try {
            updateConfiguration(configurations);
            createAllConfigurations(configurations);
        } finally {
            unlock();
        }
    }

    /**
     * Registers all available web component exporters to the registry.
     * <p>
     * This can be done only once and any following set should only return
     * false.
     *
     * @param exporters
     *            set of web component exporter classes to register
     * @return true if set successfully or false if not set
     */
    public boolean setExporters(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporters) {
        lock();
        try {
            if (exportersSet) {
                return false;
            }
            exportersSet = true;

            updateRegistry(exporters);
            return true;
        } finally {
            unlock();
        }
    }

    /**
     * Checks if {@link WebComponentExporter WebComponentExporters} have been
     * set and configurations created.
     *
     * @return {@code true} if {@link #setExporters(Set)} has been called with
     *         {@code non-null} value
     */
    public boolean hasConfigurations() {
        lock();
        try {
            return configurationMap.size() > 0;
        } finally {
            unlock();
        }
    }

    /**
     * Returns configuration annotation for embedded application.
     * <p>
     * {@link WebComponentExporter} classes may declare configuration
     * annotations. If there are several different annotations declared for
     * various exporter classes then an exception will be thrown during the
     * servlet initialization (exporter classes discovering).
     *
     * @param type
     *            the configuration annotation type
     *
     * @return an optional configuration annotation, or an empty optional if
     *         there is no configuration annotation with the given {@code type}
     */
    public <T extends Annotation> Optional<T> getEmbeddedApplicationAnnotation(
            Class<T> type) {
        lock();
        try {
            if (embeddedAppAnnotations == null) {
                return Optional.empty();
            }
            return Optional
                    .ofNullable(type.cast(embeddedAppAnnotations.get(type)));
        } finally {
            unlock();
        }
    }

    /**
     * Get an unmodifiable set containing all registered web component
     * configurations.
     *
     * @return  unmodifiable set of web component configurations in registry or
     *          an empty set
     */
    public Set<WebComponentConfiguration<? extends Component>> getConfigurations() {
        lock();
        try {
            return Collections
                    .unmodifiableSet(new HashSet<>(configurationMap.values()));
        } finally {
            unlock();
        }
    }

    /**
     * Get WebComponentRegistry instance for given servlet context.
     *
     * @param servletContext
     *            servlet context to get registry for
     * @return WebComponentRegistry instance
     */
    public static WebComponentConfigurationRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext.getAttribute(
                    WebComponentConfigurationRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext.setAttribute(
                        WebComponentConfigurationRegistry.class.getName(),
                        attribute);
            }
        }

        if (attribute instanceof WebComponentConfigurationRegistry) {
            return (WebComponentConfigurationRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private void updateConfiguration(Set<Class<?
            extends WebComponentExporter<? extends Component>>> exporterClasses) {
        assertLockHeld();

        Optional<Class<? extends Annotation>[]> annotationTypes = AnnotationReader
                .getAnnotationValueFor(WebComponentConfigurationRegistry.class,
                        EmbeddedApplicationAnnotations.class,
                        EmbeddedApplicationAnnotations::value);

        HashMap<Class<? extends Annotation>, Annotation> map = new HashMap<>();

        exporterClasses
                .forEach(exporter -> addEmbeddedApplicationAnnotation(exporter,
                        annotationTypes.get(), map));
        embeddedAppAnnotations = map;
    }

    private void addEmbeddedApplicationAnnotation(
            Class<? extends WebComponentExporter<? extends Component>> exporter,
            Class<? extends Annotation>[] types,
            Map<Class<? extends Annotation>, Annotation> map) {
        for (Class<? extends Annotation> type : types) {
            Annotation annotation = map.get(type);
            Annotation exporterAnnotation = exporter.getAnnotation(type);
            if (exporterAnnotation == null) {
                continue;
            }
            if (annotation != null && !annotation.equals(exporterAnnotation)) {
                throw new IllegalStateException(String.format(
                        "Different annotations of type '%s' are declared by the web component exporters: %s, %s",
                        type.getName(), annotation.toString(),
                        exporterAnnotation.toString()));
            }
            map.put(type, exporter.getAnnotation(type));
        }

    }

    private static WebComponentConfigurationRegistry createRegistry(
            ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentConfigurationRegistry();
        }
        Object attribute = OSGiAccess.getInstance().getOsgiServletContext()
                .getAttribute(
                        WebComponentConfigurationRegistry.class.getName());
        if (attribute instanceof OSGiWebComponentConfigurationRegistry) {
            return (WebComponentConfigurationRegistry) attribute;
        }

        return new OSGiWebComponentConfigurationRegistry();
    }

    /**
     * Constructs all web component configurations from the collected exporter
     * classes. Performs tag validation. Tags must be valid html tags and
     * unique.
     * <p>
     * Requires that {@link #lock()} has been called by the current thread.
     */
    protected void createAllConfigurations(Set<Class<?
            extends WebComponentExporter<? extends Component>>> exporterClasses) {
        assertLockHeld();

        Set<WebComponentConfiguration<? extends Component>> configurations =
                exporterClasses.stream().map(this::constructConfiguration).collect(Collectors.toSet());

        validateDistinctTagNames(configurations);
        validateTagNames(configurations);

        configurations.forEach(config -> configurationMap.put(config.getTag(),
                config));
    }


    protected WebComponentConfiguration<? extends Component> constructConfiguration(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        return new WebComponentConfigurationFactory().apply(exporterClass);
    }

    protected void lock() {
        configurationLock.lock();
    }

    protected void unlock() {
        configurationLock.unlock();
    }

    protected void assertLockHeld() {
        assert configurationLock.isHeldByCurrentThread();
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param configurationSet
     *            set of web components to validate
     */
    protected void validateTagNames(
            Set<WebComponentConfiguration<? extends Component>> configurationSet) {
        for (WebComponentConfiguration<? extends Component> configuration :
                configurationSet) {
            if (!CustomElementNameValidator
                    .isCustomElementName(configuration.getTag())) {
                throw new InvalidCustomElementNameException(String.format(
                        "Tag name '%s' given by '%s' is not a valid custom "
                                + "element name.",
                        configuration.getTag(),
                        configuration.getClass().getCanonicalName()));
            }
        }
    }

    /**
     * Validate that we have exactly one web component exporter per tag name.
     *
     * @param configurationSet
     *            set of web components to validate
     */
    protected void validateDistinctTagNames(
            Set<WebComponentConfiguration<? extends Component>> configurationSet) {
        long count = configurationSet.stream()
                .map(WebComponentConfiguration::getTag)
                .distinct().count();
        if (configurationSet.size() != count) {
            Map<String, WebComponentConfiguration<? extends Component>> items =
                    new HashMap<>();
            for (WebComponentConfiguration<? extends Component> configuration :
                    configurationSet) {
                String tag = configuration.getTag();
                if (items.containsKey(tag)) {
                    String message = String.format(
                            "Found two %s classes '%s' and '%s' for the tag "
                                    + "name '%s'. Tag must be unique.",
                            WebComponentExporter.class.getSimpleName(),
                            items.get(tag).getClass().getCanonicalName(),
                            configuration.getClass().getCanonicalName(), tag);
                    throw new IllegalArgumentException(message);
                }
                items.put(tag, configuration);
            }
        }
    }
}
