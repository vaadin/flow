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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Registry for storing available web component implementations.
 *
 * @since
 */
public class WebComponentRegistry implements Serializable {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private Map<String, Class<? extends Component>> webComponents;

    /**
     * Protected constructor for internal OSGi extensions.
     */
    protected WebComponentRegistry() {
    }

    /**
     * Get a web component class for given custom element tag if one is
     * registered.
     *
     * @param tag
     *         custom element tag
     * @return Optional containing a web component matching given tag
     */
    public Optional<Class<? extends Component>> getWebComponent(String tag) {
        Class<? extends Component> webComponent = null;
        configurationLock.lock();
        try {
            if (webComponents != null) {
                webComponent = webComponents.get(tag);

            }
        } finally {
            configurationLock.unlock();
        }
        return Optional.ofNullable(webComponent);
    }

    /**
     * Internal method for updating registry.
     *
     * @param components
     *         map of components to register
     */
    protected void updateRegistry(
            Map<String, Class<? extends Component>> components) {
        configurationLock.lock();
        try {
            Map<String, Class<? extends Component>> webComponentMap = new HashMap<>();
            for (Map.Entry<String, Class<? extends Component>> entry : components
                    .entrySet()) {
                webComponentMap.put(entry.getKey(), entry.getValue());
            }

            if (webComponentMap.isEmpty()) {
                webComponentMap = Collections.emptyMap();
            }

            webComponents = Collections.unmodifiableMap(webComponentMap);
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Register all available web components to the registry.
     * <p>
     * This can be done only once and any following set should only return
     * false.
     *
     * @param components
     *         map of components to register
     * @return true if set successfully or false if not set
     */
    public boolean setWebComponents(
            Map<String, Class<? extends Component>> components) {
        configurationLock.lock();
        try {
            if (webComponents != null) {
                return false;
            }

            updateRegistry(components);

            return true;
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Get map containing all registered web components.
     *
     * @return unmodifiable map of all web components in registry
     */
    public Map<String, Class<? extends Component>> getWebComponents() {
        configurationLock.lock();
        try {
            return webComponents;
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
    public static WebComponentRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(WebComponentRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext
                        .setAttribute(WebComponentRegistry.class.getName(),
                                attribute);
            }
        }

        if (attribute instanceof WebComponentRegistry) {
            return (WebComponentRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private static WebComponentRegistry createRegistry(ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentRegistry();
        }
        Object attribute = OSGiAccess.getInstance().getOsgiServletContext()
                .getAttribute(WebComponentRegistry.class.getName());
        if (attribute != null
                && attribute instanceof OSGiWebComponentRegistry) {
            return (WebComponentRegistry) attribute;
        }

        return new OSGiWebComponentRegistry();
    }
}
