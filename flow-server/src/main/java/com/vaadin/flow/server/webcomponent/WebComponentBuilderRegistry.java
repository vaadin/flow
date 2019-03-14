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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Registry for storing available web component builder implementations.
 *
 * @since
 */
public class WebComponentBuilderRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private Map<String, WebComponentBuilder<? extends Component>> componentBuilders;

    /**
     * Protected constructor for internal OSGi extensions.
     */
    protected WebComponentBuilderRegistry() {
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
        return Optional.ofNullable(getBuilder(tag));
    }

    /**
     * @param tag
     *          tag name of the web component
     * @return {@link WebComponentBuilder} by the tag
     */
    protected WebComponentBuilder<? extends Component> getBuilder(String tag) {
        configurationLock.lock();
        try {
            if (componentBuilders != null) {
                return componentBuilders.get(tag);
            }
        } finally {
            configurationLock.unlock();
        }
        return null;
    }

    /**
     * @param componentClass    type of the exported {@link Component}
     * @return  set of {@link WebComponentBuilder} or an empty set.
     */
    protected <T extends Component> Set<WebComponentBuilder<T>> getBuildersByComponentType(Class<T> componentClass) {
        configurationLock.lock();
        try {
            if (componentBuilders != null) {
                return componentBuilders.values().stream()
                        .filter(b -> componentClass.equals(b.getComponentClass()))
                        .map(b -> (WebComponentBuilder<T>)b)
                        .collect(Collectors.toSet());
            }
        } finally {
            configurationLock.unlock();
        }
        return Collections.emptySet();
    }

    /**
     * Internal method for updating registry.
     *
     * @param builders
     *         map of components to register
     */
    protected void updateRegistry(
            Set<WebComponentBuilder<? extends Component>> builders) {
        configurationLock.lock();
        try {
            if (builders.isEmpty()) {
                componentBuilders = Collections.unmodifiableMap(
                        Collections.emptyMap());
            }
            else {
                Map<String, WebComponentBuilder<? extends Component>> map =
                        builders.stream().collect(Collectors.toMap(
                                WebComponentBuilder::getWebComponentTag,
                                b -> b));

                componentBuilders = Collections.unmodifiableMap(map);
            }
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Register all available web components builders to the registry.
     * <p>
     * This can be done only once and any following set should only return
     * false.
     *
     * @param builders
     *         map of components to register
     * @return true if set successfully or false if not set
     */
    public boolean setBuilders(
            Set<WebComponentBuilder<? extends Component>> builders) {
        configurationLock.lock();
        try {
            if (componentBuilders != null) {
                return false;
            }

            updateRegistry(builders);
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
    public Set<WebComponentBuilder<? extends Component>> getBuilders() {
        configurationLock.lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(
                    componentBuilders.values()));
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
    public static WebComponentBuilderRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(WebComponentBuilderRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext
                        .setAttribute(WebComponentBuilderRegistry.class.getName(),
                                attribute);
            }
        }

        if (attribute instanceof WebComponentBuilderRegistry) {
            return (WebComponentBuilderRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private static WebComponentBuilderRegistry createRegistry(ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentBuilderRegistry();
        }
        Object attribute = OSGiAccess.getInstance().getOsgiServletContext()
                .getAttribute(WebComponentBuilderRegistry.class.getName());
        if (attribute != null
                && attribute instanceof OSGiWebComponentBuilderRegistry) {
            return (WebComponentBuilderRegistry) attribute;
        }

        return new OSGiWebComponentBuilderRegistry();
    }
}
