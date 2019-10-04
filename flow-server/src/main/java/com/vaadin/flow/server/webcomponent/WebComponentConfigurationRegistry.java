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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.theme.Theme;

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

/**
 * Registry for storing available web component configuration implementations.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
@EmbeddedApplicationAnnotations({Theme.class, Push.class})
public class WebComponentConfigurationRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private boolean configurationsSet = false;
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
     *         custom element tag
     * @return Optional containing a web component configuration matching given
     *         tag
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
     *         type of the exported {@link Component}
     * @param <T>
     *         component
     * @return set of {@link WebComponentConfiguration} or an empty set.
     */
    @SuppressWarnings("unchecked")
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
     *         set of web component configurations to register
     */
    protected void updateRegistry(
            Set<WebComponentConfiguration<? extends Component>> configurations) {
        lock();
        try {
            updateConfiguration(configurations);

            configurationMap =
                    new HashMap<>(configurations.stream().collect(
                            Collectors.toMap(
                                    WebComponentConfiguration::getTag,
                                    config -> config)));
        } finally {
            unlock();
        }
    }

    /**
     * Registers all available web component configurations to the registry.
     * <p>
     * This can be done only once and any following set should only return
     * false.
     *
     * @param configurations
     *         set of web component configurations to register. These
     *         configurations must have both unique and valid tag names.
     * @return {@code true} if set successfully or {@code false} if not set
     */
    public boolean setConfigurations(
            Set<WebComponentConfiguration<? extends Component>> configurations) {
        lock();
        try {
            if (configurationsSet) {
                return false;
            }
            configurationsSet = true;

            updateRegistry(configurations);
            return true;
        } finally {
            unlock();
        }
    }

    /**
     * Checks whether the registry contains any web component configurations.
     *
     * @return {@code true} if {@link #setConfigurations(Set)} has been called a
     *         non-empty set.
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
     *         the configuration annotation type
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
     * @return unmodifiable set of web component configurations in registry or
     *         an empty set
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
     * @param context
     *         {@link VaadinService} to keep the instance in
     * @return WebComponentRegistry instance
     */
    public static WebComponentConfigurationRegistry getInstance(
            VaadinContext context) {
        assert context != null;

        WebComponentConfigurationRegistry attribute =
            context.getAttribute(WebComponentConfigurationRegistry.class, WebComponentConfigurationRegistry::createRegistry);

        if (attribute == null) {
            throw new IllegalStateException(
                    "Null WebComponentConfigurationRegistry obtained from VaadinContext of type " + context.getClass().getName());
        }

        return attribute;
    }

    private void updateConfiguration(Set<WebComponentConfiguration<? extends Component>> webComponentConfigurations) {
        assertLockHeld();

        Optional<Class<? extends Annotation>[]> annotationTypes = AnnotationReader
                .getAnnotationValueFor(WebComponentConfigurationRegistry.class,
                        EmbeddedApplicationAnnotations.class,
                        EmbeddedApplicationAnnotations::value);

        HashMap<Class<? extends Annotation>, Annotation> map = new HashMap<>();

        webComponentConfigurations.forEach(config ->
                addEmbeddedApplicationAnnotation(config, annotationTypes.get(),
                        map));

        embeddedAppAnnotations = map;
    }

    private void addEmbeddedApplicationAnnotation(
            WebComponentConfiguration<? extends Component> configuration,
            Class<? extends Annotation>[] types,
            Map<Class<? extends Annotation>, Annotation> map) {
        for (Class<? extends Annotation> type : types) {
            Annotation annotation = map.get(type);
            Annotation configAnnotation =
                    configuration.getExporterClass().getAnnotation(type);
            if (configAnnotation == null) {
                continue;
            }
            if (annotation != null && !annotation.equals(configAnnotation)) {
                throw new IllegalStateException(String.format(
                        "Different annotations of type '%s' are declared by the web component exporters: %s, %s",
                        type.getName(), annotation.toString(),
                        configAnnotation.toString()));
            }
            map.put(type, configAnnotation);
        }

    }

    private static WebComponentConfigurationRegistry createRegistry() {
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

    private void lock() {
        configurationLock.lock();
    }

    private void unlock() {
        configurationLock.unlock();
    }

    private void assertLockHeld() {
        assert configurationLock.isHeldByCurrentThread();
    }
}
