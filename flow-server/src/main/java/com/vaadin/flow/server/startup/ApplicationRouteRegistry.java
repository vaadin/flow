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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteAccessDeniedError;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.ErrorRouteRegistry;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinContext;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 *
 * @since 1.3
 */
public class ApplicationRouteRegistry extends AbstractRouteRegistry
        implements ErrorRouteRegistry {

    private AtomicReference<Class<?>> pwaConfigurationClass = new AtomicReference<>();

    private final ArrayList<NavigationTargetFilter> routeFilters = new ArrayList<>();

    private final VaadinContext context;

    /**
     * Creates a new uninitialized route registry.
     *
     * @param context the Vaadin context
     */
    protected ApplicationRouteRegistry(VaadinContext context) {
        this.context = context;
        ServiceLoader.load(NavigationTargetFilter.class)
                .forEach(routeFilters::add);
    }

    /**
     * RouteRegistry wrapper class for storing the ApplicationRouteRegistry.
     */
    protected static class ApplicationRouteRegistryWrapper
            implements Serializable {
        private final ApplicationRouteRegistry registry;

        /**
         * Create a application route registry wrapper.
         *
         * @param registry
         *            application route registry to wrap
         */
        public ApplicationRouteRegistryWrapper(
                ApplicationRouteRegistry registry) {
            this.registry = registry;
        }

        /**
         * Get the application route registry.
         *
         * @return wrapped application route registry
         */
        public ApplicationRouteRegistry getRegistry() {
            return registry;
        }
    }

    /**
     * Gets the route registry for the given Vaadin context. If the Vaadin
     * context has no route registry, a new instance is created and assigned to
     * the context.
     *
     * @param context
     *            the vaadin context for which to get a route registry, not
     *            <code>null</code>
     * @return a registry instance for the given context, not <code>null</code>
     */
    public static ApplicationRouteRegistry getInstance(VaadinContext context) {
        assert context != null;

        ApplicationRouteRegistryWrapper attribute;
        synchronized (context) {
            attribute = context
                    .getAttribute(ApplicationRouteRegistryWrapper.class);

            if (attribute == null) {
                attribute = new ApplicationRouteRegistryWrapper(
                        createRegistry(context));
                context.setAttribute(attribute);
            }
        }

        return attribute.getRegistry();
    }

    @Override
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        if (routeFilters.stream().allMatch(
                filter -> filter.testNavigationTarget(navigationTarget))) {
            super.setRoute(path, navigationTarget, parentChain);
        } else {
            LoggerFactory.getLogger(ApplicationRouteRegistry.class).info(
                    "Not registering route {} because it's not valid for all registered routeFilters.",
                    navigationTarget.getName());
        }
    }

    /**
     * Set error handler navigation targets.
     * <p>
     * This can also be used to add error navigation targets that override
     * existing targets. Note! The overriding targets need to be extending the
     * existing target or they will throw.
     *
     * @param errorNavigationTargets
     *            error handler navigation targets
     */
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap = new HashMap<>();

        exceptionTargetsMap.putAll(getConfiguration().getExceptionHandlers());

        errorNavigationTargets.stream().filter(this::allErrorFiltersMatch)
                .filter(handler -> !Modifier.isAbstract(handler.getModifiers()))
                .forEach(target -> addErrorTarget(target, exceptionTargetsMap));

        initErrorTargets(exceptionTargetsMap);
    }

    private boolean allErrorFiltersMatch(Class<? extends Component> target) {
        return routeFilters.stream()
                .allMatch(filter -> filter.testErrorNavigationTarget(target));
    }

    @Override
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        if (getConfiguration().getExceptionHandlers().isEmpty()) {
            initErrorTargets(new HashMap<>());
        }
        Optional<ErrorTargetEntry> result = searchByCause(exception);
        if (!result.isPresent()) {
            result = searchBySuperType(exception);
        }
        return result;
    }

    /**
     * Check if there are registered navigation targets in the registry.
     *
     * @return true if any navigation are registered
     */
    public boolean hasNavigationTargets() {
        return !getConfiguration().getRoutes().isEmpty();
    }

    /**
     * Gets pwa configuration class.
     *
     * @return a class that has PWA-annotation.
     */
    public Class<?> getPwaConfigurationClass() {
        return pwaConfigurationClass.get();
    }

    /**
     * Sets pwa configuration class.
     *
     * Should be set along with setRoutes, for scanning of proper pwa
     * configuration class is done along route scanning. See
     * {@link AbstractRouteRegistryInitializer}.
     *
     * @param pwaClass
     *            a class that has PWA -annotation, that's to be used in service
     *            initialization.
     */
    public void setPwaConfigurationClass(Class<?> pwaClass) {
        if (pwaClass != null && pwaClass.isAnnotationPresent(PWA.class)) {
            pwaConfigurationClass.set(pwaClass);
        }
    }

    @Override
    public VaadinContext getContext() {
        return context;
    }

    /**
     * Handles an attempt to initialize already initialized route registry.
     */
    protected void handleInitializedRegistry() {
        throw new IllegalStateException(
                "Route registry has been already initialized");
    }

    private void initErrorTargets(
            Map<Class<? extends Exception>, Class<? extends Component>> map) {
        if (!map.containsKey(NotFoundException.class)) {
            map.put(NotFoundException.class, RouteNotFoundError.class);
        }
        if (!map.containsKey(AccessDeniedException.class)) {
            map.put(AccessDeniedException.class, RouteAccessDeniedError.class);
        }
        if (!map.containsKey(Exception.class)) {
            map.put(Exception.class, InternalServerError.class);
        }
        configure(configuration -> map.forEach(configuration::setErrorRoute));
    }

    private static ApplicationRouteRegistry createRegistry(
            VaadinContext context) {
        return new ApplicationRouteRegistry(context);
    }
}
