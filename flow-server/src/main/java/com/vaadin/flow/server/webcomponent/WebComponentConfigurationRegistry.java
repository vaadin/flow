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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Registry for storing available web component builder implementations.
 *
 * @since
 */
public class WebComponentConfigurationRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private HashMap<String, Class<? extends WebComponentExporter<? extends Component>>> exporterClasses = null;
    private HashMap<String, WebComponentConfigurationImpl<? extends Component>> builderCache = new HashMap<>();

    private AtomicReference<Class<? extends AbstractTheme>> webComponentsTheme = new AtomicReference<>();

    /**
     * Protected constructor for internal OSGi extensions.
     */
    protected WebComponentConfigurationRegistry() {
    }

    /**
     * Get a web component class for given custom element tag if one is
     * registered.
     *
     * @param tag
     *            custom element tag
     * @return Optional containing a web component matching given tag
     */
    public Optional<WebComponentConfiguration<? extends Component>> getConfiguration(
            String tag) {
        return Optional.ofNullable(getConfigurationInternal(tag));
    }

    /**
     * Retrieves {@link WebComponentConfigurationImpl} matching the {@code} tag.
     * If the builder is not readily available, attempts to construct it from
     * the web component exporter cache.
     *
     * @param tag
     *            tag name of the web component
     * @return {@link WebComponentConfigurationImpl} by the tag
     */
    protected WebComponentConfigurationImpl<? extends Component> getConfigurationInternal(
            String tag) {
        WebComponentConfigurationImpl<? extends Component> configuration = null;
        configurationLock.lock();
        try {
            if (exporterClasses != null) {
                populateCacheByTag(tag);
                configuration = builderCache.get(tag);
            }
        } finally {
            configurationLock.unlock();
        }
        return configuration;
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
        configurationLock.lock();
        try {
            if (exporterClasses == null) {
                return Collections.emptySet();
            }
            if (!areAllConfigurationsAvailable()) {
                populateCacheWithMissingConfigurations();
            }
            return Collections.unmodifiableSet(builderCache.values().stream()
                    .filter(b -> componentClass.equals(b.getComponentClass()))
                    .map(b -> (WebComponentConfiguration<T>) b)
                    .collect(Collectors.toSet()));

        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Internal method for updating registry.
     *
     * @param exporters
     *            map of web component exporters to register
     */
    protected void updateRegistry(
            Map<String, Class<? extends WebComponentExporter<? extends Component>>> exporters) {
        configurationLock.lock();
        try {
            if (exporters.isEmpty()) {
                exporterClasses = new HashMap<>(0);
            } else {
                exporterClasses = new HashMap<>(exporters);
            }
            updateTheme();
            // since we updated our exporter selection, we need to clear our
            // builder cache
            builderCache.clear();
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Register all available unique web component exporters to the registry.
     * The builders are instantiated lazily.
     * <p>
     * This can be done only once and any following set should only return
     * false.
     *
     * @param exporters
     *            map of web component exporters to register
     * @return true if set successfully or false if not set
     */
    public boolean setExporters(
            Map<String, Class<? extends WebComponentExporter<? extends Component>>> exporters) {
        configurationLock.lock();
        try {
            if (exporterClasses != null) {
                return false;
            }

            updateRegistry(exporters);
            return true;

        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Checks if {@link WebComponentExporter WebComponentExporters} have been
     * set.
     *
     * @return {@code true} if {@link #setExporters(Map)} has been called with
     *         {@code non-null} value
     */
    public boolean hasExporters() {
        configurationLock.lock();
        try {
            return exporterClasses != null;
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Returns a web components theme.
     * <p>
     * Only one theme may be used for the web components. The theme may be
     * declared at any web component. If there are web components which declares
     * different themes then {@link IllegalStateException} is thrown during the
     * web components scanning.
     *
     * @return
     */
    public Optional<Class<? extends AbstractTheme>> getTheme() {
        return Optional.ofNullable(webComponentsTheme.get());
    }

    /**
     * Get an unmodifiable set containing all registered web component
     * configurations.
     *
     * @return unmodifiable set of web component builders in registry or empty
     *         set
     */
    public Set<WebComponentConfiguration<? extends Component>> getConfigurations() {
        configurationLock.lock();
        try {
            if (!areAllConfigurationsAvailable()) {
                populateCacheWithMissingConfigurations();
            }

            return Collections
                    .unmodifiableSet(new HashSet<>(builderCache.values()));
        } finally {
            configurationLock.unlock();
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

    private void updateTheme() {
        assert configurationLock.isHeldByCurrentThread();
        Set<Class<? extends AbstractTheme>> themeClasses = exporterClasses
                .values().stream()
                .map(exporter -> AnnotationReader.getAnnotationValueFor(
                        exporter, Theme.class, Theme::value))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toSet());
        if (themeClasses.size() > 1) {
            throw new IllegalStateException(
                    "Several themes are declared for the web component exporters: "
                            + themeClasses.stream().map(Class::getName)
                                    .collect(Collectors.joining(", ")));
        }
        if (themeClasses.size() == 1) {
            webComponentsTheme.compareAndSet(null,
                    themeClasses.iterator().next());
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
     * Adds a builder to the builder cache if one is not already present,
     * exporters have been set, and a matching exporter is found.
     *
     * @param tag
     *            name of the web component
     */
    protected void populateCacheByTag(String tag) {
        configurationLock.lock();

        try {
            if (builderCache.containsKey(tag)) {
                return;
            }

            Class<? extends WebComponentExporter<? extends Component>> exporterClass = exporterClasses
                    .get(tag);

            if (exporterClass != null) {
                builderCache.put(tag, constructConfigurations(exporterClass));
                // remove the class reference from the data bank - it has
                // already been constructed and is no longer needed
                exporterClasses.remove(tag);
            }
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Constructs and adds all the missing configurations into the {@code
     * builderCache} based on the exporters.
     */
    protected void populateCacheWithMissingConfigurations() {
        if (exporterClasses == null) {
            return;
        }
        exporterClasses.forEach((key, value) -> builderCache.put(key,
                constructConfigurations(value)));
        // empty the exporter data bank - every builder has been constructed
        exporterClasses.clear();
    }

    protected boolean areAllConfigurationsAvailable() {
        configurationLock.lock();

        try {
            return exporterClasses != null && exporterClasses.size() == 0;
        } finally {
            configurationLock.unlock();
        }
    }

    protected WebComponentConfigurationImpl<? extends Component> constructConfigurations(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {

        Instantiator instantiator = VaadinService.getCurrent()
                .getInstantiator();

        WebComponentExporter<? extends Component> exporter = instantiator
                .getOrCreate(exporterClass);

        return new WebComponentConfigurationImpl<>(exporter);
    }
}
