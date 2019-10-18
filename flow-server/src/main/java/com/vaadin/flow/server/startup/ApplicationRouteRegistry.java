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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.shared.Registration;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 *
 * @since 1.3
 */
public class ApplicationRouteRegistry extends AbstractRouteRegistry {

    private static class OSGiRouteRegistry extends ApplicationRouteRegistry {
        private List<Registration> subscribingRegistrations = new CopyOnWriteArrayList<>();

        @Override
        public Class<?> getPwaConfigurationClass() {
            initPwa();
            return super.getPwaConfigurationClass();
        }

        @Override
        public Optional<ErrorTargetEntry> getErrorNavigationTarget(
                Exception exception) {
            initErrorTargets();
            return super.getErrorNavigationTarget(exception);
        }

        private void initErrorTargets() {
            if (!getConfiguration().getExceptionHandlers().isEmpty()) {
                return;
            }

            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            if (osgiServletContext == null
                    || !OSGiAccess.getInstance().hasInitializers()) {
                return;
            }
            OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                    new VaadinServletContext(osgiServletContext));
            if (registry.errorNavigationTargets.get() != null) {
                setErrorNavigationTargets(
                        registry.errorNavigationTargets.get());
            }
        }

        private void initPwa() {
            if (getConfiguration().getRoutes().isEmpty()) {
                return;
            }
            if (OSGiAccess.getInstance().hasInitializers()) {
                OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                        new VaadinServletContext(OSGiAccess.getInstance()
                                .getOsgiServletContext()));
                setPwaConfigurationClass(registry.getPwaConfigurationClass());
            }
        }

        private void subscribeToChanges(RouteRegistry routeRegistry) {
            subscribingRegistrations.add(routeRegistry.addRoutesChangeListener(
                    event -> update(() -> applyChange(event))));
        }

        private void applyChange(RoutesChangedEvent event) {
            final RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(this);
            event.getRemovedRoutes()
                    .forEach(routeBaseData -> routeConfiguration.removeRoute(
                            routeBaseData.getUrl(),
                            routeBaseData.getNavigationTarget()));
            event.getAddedRoutes()
                    .forEach(routeBaseData -> routeConfiguration.setRoute(
                            routeBaseData.getUrl(),
                            routeBaseData.getNavigationTarget(),
                            routeBaseData.getParentLayouts()));
        }

        private void setRoutes(List<RouteData> routes) {
            routes.forEach(routeData -> {
                setRoute(routeData.getUrl(), routeData.getNavigationTarget(),
                        routeData.getParentLayouts());
                routeData.getRouteAliases()
                        .forEach(routeAliasData -> setRoute(
                                routeAliasData.getUrl(),
                                routeAliasData.getNavigationTarget(),
                                routeAliasData.getParentLayouts()));
            });
        }
    }

    private static class OSGiDataCollector extends ApplicationRouteRegistry {

        private static final String REMOVE_ROUTE_IS_NOT_SUPPORTED_MESSAGE = "removeRoute is not supported in OSGiDataCollector";
        private AtomicReference<Set<Class<? extends Component>>> errorNavigationTargets = new AtomicReference<>();

        @Override
        protected void handleInitializedRegistry() {
            // Don't do anything in this fake internal registry
        }

        @Override
        public void setErrorNavigationTargets(
                Set<Class<? extends Component>> errorNavigationTargets) {
            if (errorNavigationTargets.isEmpty()
                    && this.errorNavigationTargets.get() == null) {
                // ignore initial empty targets avoiding error target
                // initialization it they are not yet discovered
                return;
            }
            this.errorNavigationTargets.set(errorNavigationTargets);
        }

        @Override
        public void removeRoute(Class<? extends Component> routeTarget) {
            throw new UnsupportedOperationException(
                    REMOVE_ROUTE_IS_NOT_SUPPORTED_MESSAGE);
        }

        @Override
        public void removeRoute(String path) {
            throw new UnsupportedOperationException(
                    REMOVE_ROUTE_IS_NOT_SUPPORTED_MESSAGE);
        }

        @Override
        public void removeRoute(String path,
                Class<? extends Component> navigationTarget) {
            throw new UnsupportedOperationException(
                    REMOVE_ROUTE_IS_NOT_SUPPORTED_MESSAGE);
        }
    }

    private AtomicReference<Class<?>> pwaConfigurationClass = new AtomicReference<>();
    private static final Set<Class<? extends Component>> defaultErrorHandlers = Stream
            .of(RouteNotFoundError.class, InternalServerError.class)
            .collect(Collectors.toSet());

    private final ArrayList<NavigationTargetFilter> routeFilters = new ArrayList<>();

    /**
     * Creates a new uninitialized route registry.
     */
    protected ApplicationRouteRegistry() {
        ServiceLoader.load(NavigationTargetFilter.class)
                .forEach(routeFilters::add);
    }

    /**
     * Gets the route registry for the given servlet context. If the servlet
     * context has no route registry, a new instance is created and assigned to
     * the context.
     *
     * @param context
     *         the vaadin context for which to get a route registry, not
     *         <code>null</code>
     * @return a registry instance for the given servlet context, not
     * <code>null</code>
     * @deprecated this is deprecated in favor of {@code getInstance(VaadinContext)}
     * and will be removed in a future release
     */
    @Deprecated
    public static ApplicationRouteRegistry getInstance(ServletContext context) {
        return getInstance(new VaadinServletContext(context));
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
         *         application route registry to wrap
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
     *         the vaadin context for which to get a route registry, not
     *         <code>null</code>
     * @return a registry instance for the given servlet context, not
     * <code>null</code>
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

        errorNavigationTargets.stream()
                .filter(target -> !defaultErrorHandlers.contains(target))
                .filter(this::allErrorFiltersMatch)
                .forEach(target -> addErrorTarget(target, exceptionTargetsMap));

        initErrorTargets(exceptionTargetsMap);
    }

    private boolean allErrorFiltersMatch(Class<? extends Component> target) {
        return routeFilters.stream()
                .allMatch(filter -> filter.testErrorNavigationTarget(target));
    }

    /**
     * Get a registered navigation target for given exception. First we will
     * search for a matching cause for in the exception chain and if no match
     * found search by extended type.
     *
     * @param exception
     *            exception to search error view for
     * @return optional error target entry corresponding to the given exception
     */
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

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        return getNavigationTarget(pathString, Collections.emptyList());
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {
        if (getConfiguration().hasRoute(pathString, segments)) {
            return getConfiguration().getRoute(pathString, segments);
        }
        return Optional.empty();
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
        if (!map.containsKey(Exception.class)) {
            map.put(Exception.class, InternalServerError.class);
        }
        configure(configuration -> map.forEach(configuration::setErrorRoute));
    }

    private static ApplicationRouteRegistry createRegistry(
            VaadinContext context) {
        if (context != null && ((VaadinServletContext)context).getContext() == OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new OSGiDataCollector();
        } else if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new ApplicationRouteRegistry();
        }

        OSGiRouteRegistry osgiRouteRegistry = new OSGiRouteRegistry();
        OSGiDataCollector osgiDataCollector = (OSGiDataCollector) getInstance(
                new VaadinServletContext(
                        OSGiAccess.getInstance().getOsgiServletContext()));
        osgiRouteRegistry.setRoutes(osgiDataCollector.getRegisteredRoutes());
        osgiRouteRegistry.subscribeToChanges(osgiDataCollector);
        return osgiRouteRegistry;
    }
}
