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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class GlobalRouteRegistry extends AbstractRouteRegistry {

    private static class OSGiRouteRegistry extends GlobalRouteRegistry {
        @Override
        public Class<?> getPwaConfigurationClass() {
            initPwa();
            return super.getPwaConfigurationClass();
        }

        @Override
        public List<RouteData> getRegisteredRoutes() {
            initRoutes();
            return super.getRegisteredRoutes();
        }

        @Override
        public Optional<ErrorTargetEntry> getErrorNavigationTarget(
                Exception exception) {
            initErrorTargets();
            return super.getErrorNavigationTarget(exception);
        }

        @Override
        public Optional<Class<? extends Component>> getNavigationTarget(
                String pathString) {
            initRoutes();
            return super.getNavigationTarget(pathString);
        }

        @Override
        public Optional<Class<? extends Component>> getNavigationTarget(
                String pathString, List<String> segments) {
            initRoutes();
            return super.getNavigationTarget(pathString, segments);
        }

        @Override
        public Optional<String> getTargetUrl(
                Class<? extends Component> navigationTarget) {
            initRoutes();
            return super.getTargetUrl(navigationTarget);
        }

        @Override
        public boolean hasNavigationTargets() {
            initRoutes();
            return super.hasNavigationTargets();
        }

        private void initErrorTargets() {
            if (errorNavigationTargetsInitialized()) {
                return;
            }

            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            if (osgiServletContext == null || !OSGiAccess.getInstance()
                    .hasInitializers()) {
                return;
            }
            OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                    osgiServletContext);
            if (registry.errorNavigationTargets.get() != null) {
                setErrorNavigationTargets(
                        registry.errorNavigationTargets.get());
            }
        }

        private void doInitOSGiRoutes() {
            if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
                return;
            }
            if (!OSGiAccess.getInstance().hasInitializers()) {
                return;
            }
            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                    osgiServletContext);
            RouteUtil.setNavigationTargets(registry.navigationTargets.get(), this);
        }

        private void initRoutes() {
            try {
                doInitOSGiRoutes();
            } catch (InvalidRouteConfigurationException exception) {
                assert false :
                        "Exception may not be thrown here since it should have been thrown by "
                                + OSGiDataCollector.class;
            }
        }

        private void initPwa() {
            if (!getConfiguration().isEmpty()) {
                return;
            }
            if (OSGiAccess.getInstance().hasInitializers()) {
                OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                        OSGiAccess.getInstance().getOsgiServletContext());
                setPwaConfigurationClass(registry.getPwaConfigurationClass());
            }
        }
    }

    private static class OSGiDataCollector extends GlobalRouteRegistry {

        private AtomicReference<Set<Class<? extends Component>>> navigationTargets = new AtomicReference<>();

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
    }

    private AtomicReference<Class<?>> pwaConfigurationClass = new AtomicReference<>();
    private static final Set<Class<? extends Component>> defaultErrorHandlers = Stream
            .of(RouteNotFoundError.class, InternalServerError.class)
            .collect(Collectors.toSet());

    protected final ArrayList<NavigationTargetFilter> routeFilters = new ArrayList<>();

    /**
     * Creates a new uninitialized route registry.
     */
    protected GlobalRouteRegistry() {
        ServiceLoader.load(NavigationTargetFilter.class)
                .forEach(routeFilters::add);
    }

    /**
     * Gets the route registry for the given servlet context. If the servlet
     * context has no route registry, a new instance is created and assigned to
     * the context.
     *
     * @param servletContext
     *         the servlet context for which to get a route registry, not
     *         <code>null</code>
     * @return a registry instance for the given servlet context, not
     * <code>null</code>
     */
    public static GlobalRouteRegistry getInstance(
            ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(RouteRegistry.class.getName());

            if (attribute == null) {
                attribute = createRegistry(servletContext);
                servletContext
                        .setAttribute(RouteRegistry.class.getName(), attribute);
            }
        }

        if (attribute instanceof GlobalRouteRegistry) {
            return (GlobalRouteRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    /**
     * Set error handler navigation targets.
     * <p>
     * This can also be used to add error navigation targets that override
     * existing targets. Note! The overriding targets need to be extending
     * the existing target or they will throw.
     *
     * @param errorNavigationTargets
     *         error handler navigation targets
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
     * Returns whether this registry has been initialized with error navigation
     * targets.
     *
     * @return whether this registry has been initialized with error navigation
     * targets
     */
    public boolean errorNavigationTargetsInitialized() {
        return getConfiguration().hasExceptionTargets();
    }

    /**
     * Get a registered navigation target for given exception. First we will
     * search for a matching cause for in the exception chain and if no match
     * found search by extended type.
     *
     * @param exception
     *         exception to search error view for
     * @return optional error target entry corresponding to the given exception
     */
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        if (!errorNavigationTargetsInitialized()) {
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

    public boolean hasNavigationTargets() {
        return !getConfiguration().isEmpty();
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
     * Should be set along with setNavigationTargets, for scanning of proper pwa
     * configuration class is done along route scanning. See
     * {@link AbstractRouteRegistryInitializer}.
     *
     * @param pwaClass
     *         a class that has PWA -annotation, that's to be used in service
     *         initialization.
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

    private static GlobalRouteRegistry createRegistry(ServletContext context) {
        if (context != null && context == OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new OSGiDataCollector();
        } else if (OSGiAccess.getInstance().getOsgiServletContext() == null
                || context != OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new GlobalRouteRegistry();
        }
        return new OSGiRouteRegistry();
    }
}
