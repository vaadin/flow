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
import java.util.HashMap;
import java.util.HashSet;
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
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
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
        public boolean hasRouteTo(String pathString) {
            initRoutes();
            return super.hasRouteTo(pathString);
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
            if (osgiServletContext == null
                    || !OSGiAccess.getInstance().hasInitializers()) {
                return;
            }
            OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                    osgiServletContext);
            if (registry.errorNavigationTargets.get() != null) {
                setErrorNavigationTargets(
                        registry.errorNavigationTargets.get());
            }
        }

        private void doInitOSGiRoutes()
                throws InvalidRouteConfigurationException {
            if (navigationTargetsInitialized() || OSGiAccess.getInstance()
                    .getOsgiServletContext() == null) {
                return;
            }
            if (!OSGiAccess.getInstance().hasInitializers()) {
                return;
            }
            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            OSGiDataCollector registry = (OSGiDataCollector) getInstance(
                    osgiServletContext);
            if (registry.navigationTargets.get() != null) {
                setNavigationTargets(registry.navigationTargets.get());
            }
        }

        private void initRoutes() {
            try {
                doInitOSGiRoutes();
            } catch (InvalidRouteConfigurationException exception) {
                assert false : "Exception may not be thrown here since it should have been thrown by "
                        + OSGiDataCollector.class;
            }
        }

        private void initPwa() {
            if (navigationTargetsInitialized()) {
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
        public void setNavigationTargets(
                Set<Class<? extends Component>> navigationTargets)
                throws InvalidRouteConfigurationException {
            if (navigationTargets.isEmpty()
                    && this.navigationTargets.get() == null) {
                // ignore initial empty targets avoiding routes initialization
                // it they are not yet discovered
                return;
            }
            this.navigationTargets.set(navigationTargets);
            // There is no need to execute this logic here but this method will
            // throw an exception if there are invalid routes
            super.setNavigationTargets(navigationTargets);
        }

        @Override
        public boolean navigationTargetsInitialized() {
            return false;
        }

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

    private final ArrayList<NavigationTargetFilter> routeFilters = new ArrayList<>();

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
     * Registers a set of components as navigation targets.
     * <p>
     * <strong>Note:</strong> Navigation targets can only be set once, i.e. when
     * {@link #navigationTargetsInitialized()} is {@code false}.
     *
     * @param navigationTargets
     *         set of navigation target components
     * @throws InvalidRouteConfigurationException
     *         if routing has been configured incorrectly
     */
    @Override
    public void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {

        if (navigationTargetsInitialized()) {
            throw new InvalidRouteConfigurationException(
                    "Routes have already been initialized");
        }
        registerNavigationTargets(navigationTargets);
    }

    @Override
    public void setRoute(Class<? extends Component> navigationTarget)
            throws InvalidRouteConfigurationException {
        configure(configuration -> {
            setRoute(navigationTarget, configuration);
        });
    }

    @Override
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap = new HashMap<>();
        errorNavigationTargets.stream().filter(defaultErrorHandlers::contains)
                .filter(target -> !allFiltersMatch(target))
                .forEach(target -> addErrorTarget(target, exceptionTargetsMap));

        initErrorTargets(exceptionTargetsMap);
    }

    private boolean allFiltersMatch(Class<? extends Component> target) {
        return routeFilters.stream().allMatch(
                filter -> filter.testErrorNavigationTarget(target));
    }

    private void addErrorTarget(Class<? extends Component> target,
            Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap) {
        Class<? extends Exception> exceptionType = ReflectTools
                .getGenericInterfaceType(target, HasErrorParameter.class)
                .asSubclass(Exception.class);

        if (exceptionTargetsMap.containsKey(exceptionType)) {
            handleRegisteredExceptionType(exceptionTargetsMap, target,
                    exceptionType);
        } else {
            exceptionTargetsMap.put(exceptionType, target);
        }
    }

    /**
     * Returns whether this registry has been initialized with error navigation
     * targets.
     *
     * @return whether this registry has been initialized with error navigation
     * targets
     */
    public boolean errorNavigationTargetsInitialized() {
        return routeConfiguration.hasExceptionTargets();
    }

    /**
     * Register a child handler if parent registered or leave as is if child
     * registered.
     * <p>
     * If the target is not related to the registered handler then throw
     * configuration exception as only one handler for each exception type is
     * allowed.
     *
     * @param target
     *         target being handled
     * @param exceptionType
     *         type of the handled exception
     */
    private void handleRegisteredExceptionType(
            Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap,
            Class<? extends Component> target,
            Class<? extends Exception> exceptionType) {
        Class<? extends Component> registered = exceptionTargetsMap
                .get(exceptionType);

        if (registered.isAssignableFrom(target)) {
            exceptionTargetsMap.put(exceptionType, target);
        } else if (!target.isAssignableFrom(registered)) {
            String msg = String
                    .format("Only one target for an exception should be defined. Found '%s' and '%s' for exception '%s'",
                            target.getName(), registered.getName(),
                            exceptionType.getName());
            throw new InvalidRouteLayoutConfigurationException(msg);
        }
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
        ErrorTargetEntry result = searchByCause(exception);
        if (result == null) {
            result = searchBySuperType(exception);
        }
        return Optional.ofNullable(result);
    }

    private ErrorTargetEntry searchByCause(Exception exception) {
        Class<? extends Component> targetClass = routeConfiguration
                .getExceptionHandlerByClass(exception.getClass());

        if (targetClass != null) {
            return new ErrorTargetEntry(targetClass, exception.getClass());
        }

        Throwable cause = exception.getCause();
        if (cause instanceof Exception) {
            return searchByCause((Exception) cause);
        }
        return null;
    }

    private ErrorTargetEntry searchBySuperType(Throwable exception) {
        Class<?> superClass = exception.getClass().getSuperclass();
        while (superClass != null && Exception.class
                .isAssignableFrom(superClass)) {
            Class<? extends Component> targetClass = routeConfiguration
                    .getExceptionHandlerByClass(superClass);
            if (targetClass != null) {
                return new ErrorTargetEntry(targetClass,
                        superClass.asSubclass(Exception.class));
            }
            superClass = superClass.getSuperclass();
        }

        return null;
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        return getNavigationTarget(pathString, new ArrayList<>());
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {
        if (routeConfiguration.hasRoute(pathString, segments)) {
            return routeConfiguration.getRoute(pathString, segments);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasRouteTo(String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        return routeConfiguration.hasRoute(pathString);
    }

    /**
     * Returns whether this registry has been initialized with navigation
     * targets.
     *
     * @return whether this registry has been initialized
     */
    public boolean navigationTargetsInitialized() {
        return !routeConfiguration.isEmpty();
    }

    private void registerNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {

        List<Class<? extends Component>> faulty = navigationTargets.stream()
                .filter(target -> !target.isAnnotationPresent(Route.class))
                .filter(target -> Component.class.isAssignableFrom(target))
                .collect(Collectors.toList());
        if (!faulty.isEmpty()) {
            final StringBuilder faultyClasses = new StringBuilder();
            faulty.forEach(
                    clazz -> faultyClasses.append(clazz.getName()).append(" "));
            String exceptionMessage = String
                    .format("No Route annotation is present for the given navigation target components [%s].",
                            faultyClasses.toString());
            throw new InvalidRouteConfigurationException(exceptionMessage);
        }

        configure(configuration -> {
            for (Class<? extends Component> navigationTarget : navigationTargets) {
                if (!routeFilters.stream().allMatch(filter -> filter
                        .testNavigationTarget(navigationTarget))) {
                    continue;
                }

                Set<String> routeAndRouteAliasPaths = new HashSet<>();

                String route = RouteUtil
                        .getNavigationRouteAndAliases(navigationTarget,
                                routeAndRouteAliasPaths);
                routeAndRouteAliasPaths.add(route);

                setRoute(navigationTarget, configuration);
            }
        });
    }

    @Override
    public boolean hasNavigationTargets() {
        return !routeConfiguration.isEmpty();
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
        configure(configuration -> {
            map.forEach((exception, handler) -> configuration
                    .setErrorRoute(exception, handler));
        });
    }

    private static GlobalRouteRegistry createRegistry(ServletContext context) {
        if (context != null && context == OSGiAccess.getInstance()
                .getOsgiServletContext()) {
            return new OSGiDataCollector();
        } else if (OSGiAccess.getInstance().getOsgiServletContext() == null) {
            return new GlobalRouteRegistry();
        }
        return new OSGiRouteRegistry();
    }
}
