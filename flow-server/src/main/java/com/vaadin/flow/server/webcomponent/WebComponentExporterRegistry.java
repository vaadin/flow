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
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.theme.Theme;

/**
 * Registry for storing available web component exporter implementations.
 *
 * @since
 */
@EmbeddedApplicationAnnotations({ Theme.class, Push.class })
public class WebComponentExporterRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock storageLock = new ReentrantLock(true);

    private HashMap<String, Class<? extends WebComponentExporter<? extends Component>>> exporterClasses = null;
    private HashMap<String, WebComponentExporter<? extends Component>> exporterCache =
            new HashMap<>();

    private HashMap<Class<? extends Annotation>, Annotation> embeddedAppAnnotations;

    /**
     * Protected constructor for internal OSGi extensions.
     */
    protected WebComponentExporterRegistry() {
    }

    /**
     * Get a web component class for given custom element tag if one is
     * registered.
     *
     * @param tag
     *            custom element tag
     * @return Optional containing a web component matching given tag
     */
    public Optional<WebComponentExporter<? extends Component>> getExporter(
            String tag) {
        WebComponentExporter<? extends Component> exporter = null;
        storageLock.lock();
        try {
            if (exporterClasses != null) {
                populateCacheByTag(tag);
                exporter = exporterCache.get(tag);
            }
        } finally {
            storageLock.unlock();
        }
        return Optional.ofNullable(exporter);
    }

    /**
     * Get an unmodifiable set containing all registered web component
     * exporters for a specific {@link Component} type.
     *
     * @param componentClass
     *            type of the exported {@link Component}
     * @param <T>
     *            component
     * @return set of {@link WebComponentConfiguration} or an empty set.
     */
    public <T extends Component> Set<WebComponentConfiguration<T>> getConfigurationsByComponentType(
            Class<T> componentClass) {
        storageLock.lock();
        try {
            if (exporterClasses == null) {
                return Collections.emptySet();
            }
            if (!areAllExportersAvailable()) {
                populateCacheWithMissingExporters();
            }
            return Collections.unmodifiableSet(exporterCache.values().stream()
                    .filter(b -> componentClass.equals(b.getConfiguration()
                            .getComponentClass()))
                    .map(b -> (WebComponentConfiguration<T>) b)
                    .collect(Collectors.toSet()));

        } finally {
            storageLock.unlock();
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
        storageLock.lock();
        try {
            if (exporters.isEmpty()) {
                exporterClasses = new HashMap<>(0);
            } else {
                exporterClasses = new HashMap<>(exporters);
            }
            updateConfiguration();
            // since we updated our exporter selection, we need to clear our
            // builder cache
            exporterCache.clear();
        } finally {
            storageLock.unlock();
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
        storageLock.lock();
        try {
            if (exporterClasses != null) {
                return false;
            }

            updateRegistry(exporters);
            return true;

        } finally {
            storageLock.unlock();
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
        storageLock.lock();
        try {
            return exporterClasses != null;
        } finally {
            storageLock.unlock();
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
        storageLock.lock();
        try {
            if (embeddedAppAnnotations == null) {
                return Optional.empty();
            }
            return Optional
                    .ofNullable(type.cast(embeddedAppAnnotations.get(type)));
        } finally {
            storageLock.unlock();
        }
    }

    /**
     * Get an unmodifiable set containing all registered web component
     * exporters.
     *
     * @return unmodifiable set of web component builders in registry or empty
     *         set
     */
    public Set<WebComponentExporter<? extends Component>> getExporters() {
        storageLock.lock();
        try {
            if (!areAllExportersAvailable()) {
                populateCacheWithMissingExporters();
            }

            return Collections
                    .unmodifiableSet(new HashSet<>(exporterCache.values()));
        } finally {
            storageLock.unlock();
        }
    }

    /**
     * Get WebComponentRegistry instance for given servlet context.
     *
     * @param servletContext
     *            servlet context to get registry for
     * @return WebComponentRegistry instance
     */
    public static WebComponentExporterRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext.getAttribute(
                    WebComponentExporterRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext.setAttribute(
                        WebComponentExporterRegistry.class.getName(),
                        attribute);
            }
        }

        if (attribute instanceof WebComponentExporterRegistry) {
            return (WebComponentExporterRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private void updateConfiguration() {
        assert storageLock.isHeldByCurrentThread();

        Optional<Class<? extends Annotation>[]> annotationTypes = AnnotationReader
                .getAnnotationValueFor(WebComponentExporterRegistry.class,
                        EmbeddedApplicationAnnotations.class,
                        EmbeddedApplicationAnnotations::value);

        HashMap<Class<? extends Annotation>, Annotation> map = new HashMap<>();

        exporterClasses.values().stream()
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

    private static WebComponentExporterRegistry createRegistry(
            ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentExporterRegistry();
        }
        Object attribute = OSGiAccess.getInstance().getOsgiServletContext()
                .getAttribute(
                        WebComponentExporterRegistry.class.getName());
        if (attribute instanceof OSGiWebComponentExporterRegistry) {
            return (WebComponentExporterRegistry) attribute;
        }

        return new OSGiWebComponentExporterRegistry();
    }

    /**
     * Adds a builder to the builder cache if one is not already present,
     * exporters have been set, and a matching exporter is found.
     *
     * @param tag
     *            name of the web component
     */
    protected void populateCacheByTag(String tag) {
        storageLock.lock();

        try {
            if (exporterCache.containsKey(tag)) {
                return;
            }

            Class<? extends WebComponentExporter<? extends Component>> exporterClass = exporterClasses
                    .get(tag);

            if (exporterClass != null) {
                exporterCache.put(tag, constructExporter(exporterClass));
                // remove the class reference from the data bank - it has
                // already been constructed and is no longer needed
                exporterClasses.remove(tag);
            }
        } finally {
            storageLock.unlock();
        }
    }

    /**
     * Constructs and adds all the missing configurations into the {@code
     * builderCache} based on the exporters.
     */
    protected void populateCacheWithMissingExporters() {
        if (exporterClasses == null) {
            return;
        }
        exporterClasses.forEach((key, value) -> exporterCache.put(key,
                constructExporter(value)));
        // empty the exporter data bank - every builder has been constructed
        exporterClasses.clear();
    }

    protected boolean areAllExportersAvailable() {
        storageLock.lock();

        try {
            return exporterClasses != null && exporterClasses.size() == 0;
        } finally {
            storageLock.unlock();
        }
    }

    protected WebComponentExporter<? extends Component> constructExporter(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {

        Instantiator instantiator = VaadinService.getCurrent()
                .getInstantiator();

        return instantiator
                .getOrCreate(exporterClass);
    }
}
