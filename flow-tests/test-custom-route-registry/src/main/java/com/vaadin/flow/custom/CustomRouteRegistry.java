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
package com.vaadin.flow.custom;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.ErrorRouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Custom routeRegistry implementation for use instead of
 * ApplicationRouteRegistry.
 *
 * Also implements ErrorRouteRegistry for showing error views if registered.
 */
public class CustomRouteRegistry extends AbstractRouteRegistry
        implements ErrorRouteRegistry {

    private final VaadinContext context;

    private CustomRouteRegistry(VaadinContext context) {
        this.context = context;
    }

    public static CustomRouteRegistry getInstance(VaadinContext context) {
        assert context != null;

        RegistryWrapper attribute;
        synchronized (context) {
            attribute = context.getAttribute(RegistryWrapper.class);

            if (attribute == null) {
                final CustomRouteRegistry registry = new CustomRouteRegistry(
                        context);
                attribute = new RegistryWrapper(registry);
                context.setAttribute(attribute);

                // Get collected application routes
                final ApplicationRouteRegistry instance = ApplicationRouteRegistry
                        .getInstance(context);
                instance.getRegisteredRoutes().forEach(routeData -> {
                    registry.setRoute(routeData.getTemplate(),
                            routeData.getNavigationTarget(),
                            routeData.getParentLayouts());
                });

                // Add only NotFoundException and Exception ignoring other error
                // views collected
                Map<Class<? extends Exception>, Class<? extends Component>> map = new HashMap<>();
                map.put(NotFoundException.class, CustomNotFoundView.class);
                registry.configure(configuration -> map
                        .forEach(configuration::setErrorRoute));
            }
        }

        return attribute.getRegistry();
    }

    @Override
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        Optional<ErrorTargetEntry> result = searchByCause(exception);
        if (!result.isPresent()) {
            result = searchBySuperType(exception);
        }
        return result;
    }

    protected static class RegistryWrapper implements Serializable {
        private final CustomRouteRegistry registry;

        public RegistryWrapper(CustomRouteRegistry registry) {
            this.registry = registry;
        }

        public CustomRouteRegistry getRegistry() {
            return registry;
        }
    }

    @Override
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        getLogger().info("Added target '{}' for route '{}'",
                navigationTarget.getSimpleName(), path);
        super.setRoute(path, navigationTarget, parentChain);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(CustomRouteRegistry.class);
    }

    @Override
    public VaadinContext getContext() {
        return context;
    }
}
