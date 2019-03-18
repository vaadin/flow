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
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.osgi.OSGiAccess;

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

    private Map<String, Class<? extends WebComponentExporter<?
            extends Component>>> exporterClasses;
    private Map<String, WebComponentConfigurationImpl<? extends Component>>
            builderCache = new HashMap<>();

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
     *         custom element tag
     * @return Optional containing a web component matching given tag
     */
    public Optional<WebComponentConfiguration<? extends Component>> getConfiguration(String tag) {
        return Optional.ofNullable(getConfigurationInternal(tag));
    }

    /**
     * Retrieves {@link WebComponentConfigurationImpl} matching the {@code} tag. If the
     * builder is not readily available, attempts to construct it from the
     * web component exporter cache.
     *
     * @param tag
     *          tag name of the web component
     * @return {@link WebComponentConfigurationImpl} by the tag
     */
    protected WebComponentConfigurationImpl<? extends Component> getConfigurationInternal(String tag) {
        WebComponentConfigurationImpl<? extends Component> builder = null;
        configurationLock.lock();
        try {
            if (exporterClasses != null) {
                populateCacheByTag(tag);
                builder = builderCache.get(tag);
            }
        } finally {
            configurationLock.unlock();
        }
        return builder;
    }

    /**
     * @param componentClass    type of the exported {@link Component}
     * @param <T>               component
     * @return  set of {@link WebComponentConfiguration} or an empty set.
     */
    public <T extends Component> Set<WebComponentConfiguration<T>> getConfigurationsByComponentType(Class<T> componentClass) {
        configurationLock.lock();
        try {
            if (exporterClasses == null) {
                return Collections.emptySet();
            }
            if (!allBuildersAvailable()) {
                populateCacheWithMissingBuilders();
            }
            return builderCache.values().stream()
                    .filter(b -> componentClass.equals(b.getComponentClass()))
                    .map(b -> (WebComponentConfiguration<T>)b)
                    .collect(Collectors.toSet());

        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Internal method for updating registry.
     *
     * @param exporters
     *         map of web component exporters to register
     */
    protected void updateRegistry(Map<String, Class<?
            extends WebComponentExporter<? extends Component>>> exporters) {
        configurationLock.lock();
        try {
            if (exporters.isEmpty()) {
                exporterClasses = Collections.emptyMap();
            } else {
                exporterClasses = new HashMap<>(exporters);
            }
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
     *         map of web component exporters to register
     * @return true if set successfully or false if not set
     */
    public boolean setExporters(Map<String, Class<?
            extends WebComponentExporter<? extends Component>>> exporters) {
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
     * Get map containing all registered web component builders.
     *
     * @return unmodifiable set of web component builders in registry
     */
    public Set<WebComponentConfiguration<? extends Component>> getConfigurations() {
        configurationLock.lock();
        try {
            if (!allBuildersAvailable()) {
                populateCacheWithMissingBuilders();
            }

            return Collections.unmodifiableSet(new HashSet<>(
                    builderCache.values()));
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Get WebComponentRegistry instance for given servlet context.
     *
     * @param servletContext
     *         servlet context to get registry for
     * @return WebComponentRegistry instance
     */
    public static WebComponentConfigurationRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(WebComponentConfigurationRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext
                        .setAttribute(WebComponentConfigurationRegistry.class.getName(),
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

    private static WebComponentConfigurationRegistry createRegistry(ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentConfigurationRegistry();
        }
        Object attribute = OSGiAccess.getInstance().getOsgiServletContext()
                .getAttribute(WebComponentConfigurationRegistry.class.getName());
        if (attribute != null
                && attribute instanceof OSGiWebComponentConfigurationRegistry) {
            return (WebComponentConfigurationRegistry) attribute;
        }

        return new OSGiWebComponentConfigurationRegistry();
    }

    /**
     * Adds a builder to the builder cache if one is not already present,
     * exporters have been set, and a matching exporter is found.
     *
     * @param tag   name of the web component
     */
    protected void populateCacheByTag(String tag) {
        configurationLock.lock();

        try {
            if (builderCache.containsKey(tag)) {
                return;
            }

            Class<? extends WebComponentExporter<? extends Component>> exporterClass
                    = exporterClasses.get(tag);

            if (exporterClass != null) {
                builderCache.put(tag, constructBuilder(tag, exporterClass));
                // remove the class reference from the data bank - it has
                // already been constructed and is no longer needed
                exporterClasses.remove(tag);
            }
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Constructs and adds all the missing builders into the {@code
     * builderCache} based on the exporters.
     */
    protected void populateCacheWithMissingBuilders() {
        exporterClasses.forEach((key, value) -> builderCache.put(key,
                constructBuilder(key, value)));
        // empty the exporter data bank - every builder has been constructed
        exporterClasses.clear();
    }

    protected boolean allBuildersAvailable() {
        configurationLock.lock();

        try {
            return exporterClasses != null &&
                    exporterClasses.size() == 0;
        } finally {
            configurationLock.unlock();
        }
    }

    protected WebComponentConfigurationImpl<? extends Component> constructBuilder(
            String tag, Class<? extends WebComponentExporter<?
            extends Component>> exporterClass) {

        Instantiator instantiator =
                VaadinService.getCurrent().getInstantiator();

        WebComponentExporter<? extends Component> exporter =
                instantiator.getOrCreate(exporterClass);

        return new WebComponentConfigurationImpl<>(tag, exporter);
    }
}
