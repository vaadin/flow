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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.osgi.OSGiAccess;

public class WebComponentRegistry {

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    private AtomicReference<Map<String, Class<? extends Component>>> webComponents = new AtomicReference<>();

    protected WebComponentRegistry() {
    }

    public Optional<Class<? extends Component>> getWebComponent(String tag) {
        Class<? extends Component> webComponent = null;
        if (webComponents.get() != null) {
            webComponent = webComponents.get().get(tag);
        }
        return Optional.ofNullable(webComponent);
    }

    public void setWebComponents(
            Map<String, Class<? extends Component>> components) {
        configurationLock.lock();
        try {
            if (webComponents.get() != null) {
                return;
            }

            Map<String, Class<? extends Component>> webComponentMap = new HashMap<>();
            for (Map.Entry<String, Class<? extends Component>> entry : components
                    .entrySet()) {
                webComponentMap.put(entry.getKey(), entry.getValue());
            }

            if (webComponentMap.isEmpty()) {
                webComponentMap = Collections.emptyMap();
            }

            if (!webComponents.compareAndSet(null,
                    Collections.unmodifiableMap(webComponentMap))) {
                throw new IllegalStateException(
                        "WebComponents have already been initialized");
            }
        } finally {
            configurationLock.unlock();
        }
    }

    public Map<String, Class<? extends Component>> getWebComponents() {
        return webComponents.get();
    }

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
        if (context != null && context == OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new OSGiWebComponentDataCollector();
        } else if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new WebComponentRegistry();
        }
        return new OSGiWebComponentRegistry();
    }
}
