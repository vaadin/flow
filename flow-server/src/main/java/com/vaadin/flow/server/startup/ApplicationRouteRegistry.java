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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class ApplicationRouteRegistry extends AbstractRouteRegistry {

    private static class OSGiRouteRegistry extends ApplicationRouteRegistry {
        @Override
        public Class<?> getPwaConfigurationClass() {
            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return ((ApplicationRouteRegistry) osgiRegistry.get())
                        .getPwaConfigurationClass();
            }

            return super.getPwaConfigurationClass();
        }

        @Override
        public List<RouteData> getRegisteredRoutes() {
            List<RouteData> routes = new ArrayList<>(
                    super.getRegisteredRoutes());
            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                List<RouteData> registeredRoutes = osgiRegistry.get()
                        .getRegisteredRoutes();
                if (!registeredRoutes.isEmpty()) {
                    Set<String> collect = routes.stream().map(RouteData::getUrl)
                            .collect(Collectors.toSet());
                    registeredRoutes.stream()
                            .filter(data -> !collect.contains(data.getUrl()))
                            .forEach(routes::add);
                }
            }

            return routes;
        }

        @Override
        public Optional<ErrorTargetEntry> getErrorNavigationTarget(
                Exception exception) {

            Optional<ErrorTargetEntry> errorNavigationTarget = super
                    .getErrorNavigationTarget(exception);
            Optional<ErrorTargetEntry> osgiErrorNavigationTarget = Optional
                    .empty();

            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                osgiErrorNavigationTarget = ((ApplicationRouteRegistry) osgiRegistry
                        .get()).getErrorNavigationTarget(exception);
            }
            if (errorNavigationTarget.isPresent() || osgiErrorNavigationTarget
                    .isPresent()) {
                if (errorNavigationTarget.isPresent() && errorNavigationTarget
                        .get().getHandledExceptionType()
                        .equals(exception.getClass())) {
                    return errorNavigationTarget;
                } else if (osgiErrorNavigationTarget.isPresent()
                        && osgiErrorNavigationTarget.get()
                        .getHandledExceptionType()
                        .equals(exception.getClass())) {
                    return osgiErrorNavigationTarget;
                }

                if (errorNavigationTarget.isPresent()) {
                    return errorNavigationTarget;
                }
                return osgiErrorNavigationTarget;
            }
            return Optional.empty();
        }

        @Override
        public Optional<Class<? extends Component>> getNavigationTarget(
                String pathString) {

            Optional<Class<? extends Component>> navigationTarget = super
                    .getNavigationTarget(pathString);
            if (navigationTarget.isPresent()) {
                return navigationTarget;
            }

            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return osgiRegistry.get().getNavigationTarget(pathString);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Class<? extends Component>> getNavigationTarget(
                String pathString, List<String> segments) {
            Optional<Class<? extends Component>> navigationTarget = super
                    .getNavigationTarget(pathString, segments);
            if (navigationTarget.isPresent()) {
                return navigationTarget;
            }

            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return osgiRegistry.get()
                        .getNavigationTarget(pathString, segments);
            }
            return Optional.empty();
        }

        @Override
        public Optional<String> getTargetUrl(
                Class<? extends Component> navigationTarget) {
            Optional<String> targetUrl = super.getTargetUrl(navigationTarget);
            if (targetUrl.isPresent()) {
                return targetUrl;
            }
            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return osgiRegistry.get().getTargetUrl(navigationTarget);
            }
            return Optional.empty();
        }

        @Override
        public boolean hasNavigationTargets() {
            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return super.hasNavigationTargets()
                        || ((ApplicationRouteRegistry) osgiRegistry.get())
                        .hasNavigationTargets();
            }
            return super.hasNavigationTargets();
        }

        @Override
        public List<Class<? extends RouterLayout>> getRouteLayouts(String path,
                Class<? extends Component> navigationTarget) {
            if (getConfiguration().hasRoute(path)) {
                return super.getRouteLayouts(path, navigationTarget);
            }

            Optional<RouteRegistry> osgiRegistry = getOSGiRegistry();
            if (osgiRegistry.isPresent()) {
                return osgiRegistry.get()
                        .getRouteLayouts(path, navigationTarget);
            }
            return Collections.emptyList();
        }

        private Optional<RouteRegistry> getOSGiRegistry() {
            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            if (osgiServletContext == null || !OSGiAccess.getInstance()
                    .hasInitializers()) {
                return Optional.empty();
            }
            return Optional.ofNullable(getInstance(osgiServletContext));
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
     * @param servletContext
     *         the servlet context for which to get a route registry, not
     *         <code>null</code>
     * @return a registry instance for the given servlet context, not
     * <code>null</code>
     */
    public static ApplicationRouteRegistry getInstance(
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

        if (attribute instanceof ApplicationRouteRegistry) {
            return (ApplicationRouteRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    @Override
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        if (routeFilters.stream().allMatch(
                filter -> filter.testNavigationTarget(navigationTarget))) {
            super.setRoute(path, navigationTarget, parentChain);
        } else {
            LoggerFactory.getLogger(ApplicationRouteRegistry.class)
                    .info("Not registering route {} because it's not valid for all registered routeFilters.",
                            navigationTarget.getName());
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
            ServletContext context) {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null
                && context != OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new OSGiRouteRegistry();
        }
        return new ApplicationRouteRegistry();
    }
}
