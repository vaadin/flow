/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouterUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class RouteRegistry implements Serializable {

    /**
     * A pair of a navigation target for handling exceptions and the exception
     * type handled by the navigation target.
     */
    public static class ErrorTargetEntry implements Serializable {
        private final Class<? extends Component> navigationTarget;
        private final Class<? extends Exception> handledExceptionType;

        /**
         * Creates a new new entry with the given navigation target type and
         * exception type.
         *
         * @param navigationTarget
         *            the navigation target type, not <code>null</code>
         * @param handledExceptionType
         *            the exception type handled by the navigation target, not
         *            <code>null</code>
         */
        public ErrorTargetEntry(Class<? extends Component> navigationTarget,
                Class<? extends Exception> handledExceptionType) {
            assert navigationTarget != null;
            assert handledExceptionType != null;

            this.navigationTarget = navigationTarget;
            this.handledExceptionType = handledExceptionType;
        }

        /**
         * Gets the navigation target type.
         *
         * @return the navigation target type, not <code>null</code>
         */
        public Class<? extends Component> getNavigationTarget() {
            return navigationTarget;
        }

        /**
         * Gets the exception type handled by the navigation target.
         *
         * @return the exception type, not <code>null</code>
         */
        public Class<? extends Exception> getHandledExceptionType() {
            return handledExceptionType;
        }
    }

    private Class<?> pwaConfigurationClass;

    private static final ThemeDefinition LUMO_CLASS_IF_AVAILABLE = loadLumoClassIfAvailable();
    private static final Set<Class<? extends Component>> defaultErrorHandlers = Stream
            .of(RouteNotFoundError.class, InternalServerError.class)
            .collect(Collectors.toSet());

    private final ArrayList<NavigationTargetFilter> routeFilters = new ArrayList<>();

    private final AtomicReference<Map<String, RouteTarget>> routes = new AtomicReference<>();
    private final AtomicReference<Map<Class<? extends Component>, String>> targetRoutes = new AtomicReference<>();
    private final AtomicReference<Map<Class<? extends Exception>, Class<? extends Component>>> exceptionTargets = new AtomicReference<>();
    private final AtomicReference<List<RouteData>> routeData = new AtomicReference<>();

    /**
     * Creates a new uninitialized route registry.
     */
    protected RouteRegistry() {
        ServiceLoader.load(NavigationTargetFilter.class)
                .forEach(routeFilters::add);
    }

    /**
     * Loads the Lumo theme class from the classpath if it is available.
     *
     * @return the Lumo ThemeDefinition, or <code>null</code> if it is not
     *         available in the classpath
     */
    private static final ThemeDefinition loadLumoClassIfAvailable() {
        try {
            Class<? extends AbstractTheme> theme = (Class<? extends AbstractTheme>) Class
                    .forName("com.vaadin.flow.theme.lumo.Lumo");
            return new ThemeDefinition(theme, "");
        } catch (ClassNotFoundException e) {
            // ignore, the Lumo class is not available in the classpath
            Logger logger = LoggerFactory
                    .getLogger(RouteRegistry.class.getName());
            logger.trace(
                    "Lumo theme is not present in the classpath. The application will not use any default theme.",
                    e);
        }
        return null;
    }

    /**
     * Gets the route registry for the given servlet context. If the servlet
     * context has no route registry, a new instance is created and assigned to
     * the context.
     *
     * @param servletContext
     *            the servlet context for which to get a route registry, not
     *            <code>null</code>
     *
     * @return a registry instance for the given servlet context, not
     *         <code>null</code>
     */
    public static RouteRegistry getInstance(ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(RouteRegistry.class.getName());

            if (attribute == null) {
                attribute = new RouteRegistry();
                servletContext.setAttribute(RouteRegistry.class.getName(),
                        attribute);
            }
        }

        if (attribute instanceof RouteRegistry) {
            return (RouteRegistry) attribute;
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
     *            set of navigation target components
     * @throws InvalidRouteConfigurationException
     *             if routing has been configured incorrectly
     */
    public void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {

        if (navigationTargetsInitialized()) {
            throw new InvalidRouteConfigurationException(
                    "Routes have already been initialized");
        }
        registerNavigationTargets(navigationTargets);
    }

    /**
     * Set error handler navigation targets.
     *
     * @param errorNavigationTargets
     *            error handler navigation targets
     */
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {
        Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap = new HashMap<>();
        errorNavigationTargets.removeAll(defaultErrorHandlers);
        for (Class<? extends Component> target : errorNavigationTargets) {
            if (!routeFilters.stream().allMatch(
                    filter -> filter.testErrorNavigationTarget(target))) {
                continue;
            }

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
        initErrorTargets(exceptionTargetsMap);
    }

    /**
     * Get the {@link RouteData} for all registered navigation targets.
     *
     * @return list of routes available for this registry
     */
    public List<RouteData> getRegisteredRoutes() {
        // Build and collect only on first request
        if (routeData.get() == null) {
            List<RouteData> registeredRoutes = new ArrayList<>();
            Map<Class<? extends Component>, String> targetRouteMap = targetRoutes
                    .get();
            if (targetRouteMap != null) {
                targetRouteMap.forEach((target, url) -> {
                    List<Class<?>> parameters = getRouteParameters(target);

                    RouteData route = new RouteData(getParentLayout(target),
                            url, parameters, target);
                    registeredRoutes.add(route);
                });
            }

            Collections.sort(registeredRoutes);

            routeData.compareAndSet(null,
                    Collections.unmodifiableList(registeredRoutes));
        }

        return routeData.get();
    }

    private Class<? extends RouterLayout> getParentLayout(Class<?> target) {
        return AnnotationReader.getAnnotationFor(target, Route.class)
                .map(Route::layout).orElse(null);
    }

    private List<Class<?>> getRouteParameters(
            Class<? extends Component> target) {
        List<Class<?>> parameters = new ArrayList<>();
        if (HasUrlParameter.class.isAssignableFrom(target)) {
            Class<?> genericInterfaceType = ReflectTools
                    .getGenericInterfaceType(target, HasUrlParameter.class);
            parameters.add(genericInterfaceType);
        }

        return parameters;
    }

    /**
     * Returns whether this registry has been initialized with error navigation
     * targets.
     *
     * @return whether this registry has been initialized with error navigation
     *         targets
     */
    public boolean errorNavigationTargetsInitialized() {
        return exceptionTargets.get() != null;
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
     *            target being handled
     * @param exceptionType
     *            type of the handled exception
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
            String msg = String.format(
                    "Only one target for an exception should be defined. Found '%s' and '%s' for exception '%s'",
                    target.getName(), registered.getName(),
                    exceptionType.getName());
            throw new InvalidRouteLayoutConfigurationException(msg);
        }
    }

    private void initErrorTargets(
            Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetsMap) {
        if (!exceptionTargetsMap.containsKey(NotFoundException.class)) {
            exceptionTargetsMap.put(NotFoundException.class,
                    RouteNotFoundError.class);
        }
        if (!exceptionTargetsMap.containsKey(Exception.class)) {
            exceptionTargetsMap.put(Exception.class, InternalServerError.class);
        }
        if (!exceptionTargets.compareAndSet(null, exceptionTargetsMap)) {
            throw new IllegalStateException(
                    "Exception targets has been already initialized");
        }
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
        Class<? extends Component> targetClass = exceptionTargets.get()
                .get(exception.getClass());

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
        while (superClass != null
                && Exception.class.isAssignableFrom(superClass)) {
            Class<? extends Component> targetClass = exceptionTargets.get()
                    .get(superClass);
            if (targetClass != null) {
                return new ErrorTargetEntry(targetClass,
                        superClass.asSubclass(Exception.class));
            }
            superClass = superClass.getSuperclass();
        }

        return null;
    }

    /**
     * Gets the optional navigation target class for a given Location. Returns
     * an empty optional if no navigation target corresponds to the given
     * Location.
     *
     * @see Location
     *
     * @param pathString
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given
     *         location
     */
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        return getNavigationTarget(pathString, new ArrayList<>());
    }

    /**
     * Gets the optional navigation target class for a given Location matching
     * with path segments.
     *
     *
     * @see Location
     *
     * @param pathString
     *            path to get navigation target for, not {@code null}
     * @param segments
     *            segments given for path
     * @return optional navigation target corresponding to the given location
     *         with given segments if any applicable targets found.
     */
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {
        if (hasRouteTo(pathString)) {
            return Optional.ofNullable(
                    getRoutes().get(pathString).getTarget(segments));
        }
        return Optional.empty();
    }

    /**
     * Checks if the registry contains a route to the given path.
     *
     * @param pathString
     *            path to get navigation target for, not {@code null}
     * @return true if the registry contains a route to the given path, false
     *         otherwise.
     */
    public boolean hasRouteTo(String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        return getRoutes().containsKey(pathString);
    }

    /**
     * Get the url string for given navigation target.
     *
     * @param navigationTarget
     *            navigation target to get registered route for, not
     *            {@code null}
     * @return optional navigation target url string
     */
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget, "Target must not be null.");
        return Optional.ofNullable(collectRequiredParameters(navigationTarget));
    }

    /**
     * Append any required parameters as /{param_class} to the route.
     *
     * @param navigationTarget
     *            navigation target to generate url for
     * @return route with required parameters
     */
    private String collectRequiredParameters(
            Class<? extends Component> navigationTarget) {
        StringBuilder route = new StringBuilder(
                targetRoutes.get().get(navigationTarget));

        List<Class<?>> routeParameters = getRouteParameters(navigationTarget);

        if (!routeParameters.isEmpty()) {
            routeParameters.forEach(param -> route.append("/{")
                    .append(param.getSimpleName()).append("}"));
        }
        return route.toString();
    }

    /**
     * Returns whether this registry has been initialized with navigation
     * targets.
     *
     * @return whether this registry has been initialized
     */
    public boolean navigationTargetsInitialized() {
        return routes.get() != null;
    }

    /**
     * Collect the whole route for the navigation target.
     * <p>
     * The whole route is composed of the Route annotation and any
     * ParentLayout:@RoutePrefix that may be in the navigation chain.
     *
     * @param navigationTarget
     *            navigation target to get chain route for
     * @return full navigation route
     */
    private String getNavigationRoute(Class<?> navigationTarget,
            Collection<String> aliases) {
        Route annotation = navigationTarget.getAnnotation(Route.class);

        aliases.addAll(getRouteAliases(navigationTarget));

        return RouterUtil.getRoutePath(navigationTarget, annotation);
    }

    private Collection<String> getRouteAliases(Class<?> navigationTarget) {
        List<String> aliases = new ArrayList<>();
        for (RouteAlias alias : navigationTarget
                .getAnnotationsByType(RouteAlias.class)) {
            aliases.add(RouterUtil.getRouteAliasPath(navigationTarget, alias));
        }
        return aliases;
    }

    private void registerNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        Map<String, RouteTarget> routesMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutesMap = new HashMap<>();
        for (Class<? extends Component> navigationTarget : navigationTargets) {
            if (!navigationTarget.isAnnotationPresent(Route.class)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "No Route annotation is present for the given "
                                + "navigation target component '%s'.",
                        navigationTarget.getName()));
            }

            if (!routeFilters.stream().allMatch(
                    filter -> filter.testNavigationTarget(navigationTarget))) {
                continue;
            }

            Set<String> paths = new HashSet<>();
            String route = getNavigationRoute(navigationTarget, paths);
            paths.add(route);

            targetRoutesMap.put(navigationTarget, route);
            addRoute(routesMap, navigationTarget, paths);
        }
        if (!routes.compareAndSet(null,
                Collections.unmodifiableMap(routesMap))) {
            throw new IllegalStateException(
                    "Route registry has been already initialized");
        }
        if (!targetRoutes.compareAndSet(null,
                Collections.unmodifiableMap(targetRoutesMap))) {
            throw new IllegalStateException(
                    "Route registry has been already initialized");
        }
    }

    private void addRoute(Map<String, RouteTarget> routesMap,
            Class<? extends Component> navigationTarget,
            Collection<String> paths)
            throws InvalidRouteConfigurationException {
        Logger logger = LoggerFactory.getLogger(RouteRegistry.class.getName());
        for (String path : paths) {
            RouteTarget routeTarget;
            if (routesMap.containsKey(path)) {
                routeTarget = routesMap.get(path);
                routeTarget.addRoute(navigationTarget);
            } else {
                logger.debug(
                        "Registering route '{}' to navigation target '{}'.",
                        path, navigationTarget.getName());

                routeTarget = new RouteTarget(navigationTarget);
                routesMap.put(path, routeTarget);
            }
            routeTarget.setThemeFor(navigationTarget,
                    findThemeForNavigationTarget(navigationTarget, path));
        }
    }

    private ThemeDefinition findThemeForNavigationTarget(
            Class<?> navigationTarget, String path) {

        if (navigationTarget == null) {
            return LUMO_CLASS_IF_AVAILABLE;
        }

        Class<? extends RouterLayout> topParentLayout = RouterUtil
                .getTopParentLayout(navigationTarget, path);

        Class<?> target = topParentLayout == null ? navigationTarget
                : topParentLayout;

        Optional<Theme> themeAnnotation = AnnotationReader
                .getAnnotationFor(target, Theme.class);

        if (themeAnnotation.isPresent()) {
            return new ThemeDefinition(themeAnnotation.get());
        }

        if (!AnnotationReader.getAnnotationFor(target, NoTheme.class)
                .isPresent()) {
            return LUMO_CLASS_IF_AVAILABLE;
        }

        return null;
    }

    /**
     * Checks whether any navigation targets have been registered.
     *
     * @return <code>true</code> if at least one navigation target is
     *         registered; otherwise <code>false</code>
     */
    public boolean hasNavigationTargets() {
        return !getRoutes().isEmpty();
    }

    private Map<String, RouteTarget> getRoutes() {
        Map<String, RouteTarget> map = routes.get();
        if (map == null) {
            return Collections.emptyMap();
        }
        return map;
    }

    /**
     * Check if there are any registered routes.
     *
     * @return true if we have registered routes
     */
    public boolean hasRoutes() {
        return navigationTargetsInitialized() && !routes.get().isEmpty();
    }

    /**
     * Gets the {@link ThemeDefinition} associated with the given navigation
     * target, if any. The theme is defined by using the {@link Theme}
     * annotation on the navigation target class.
     * <p>
     * If no {@link Theme} and {@link NoTheme} annotation are used, by default
     * the {@code com.vaadin.flow.theme.lumo.Lumo} class is used (if present on
     * the classpath).
     *
     * @param navigationTarget
     *            the navigation target class
     * @param path
     *            the resolved route path so we can determine what the rendered
     *            target is for
     * @return the associated ThemeDefinition, or empty if none is defined and
     *         the Lumo class is not in the classpath, or if the NoTheme
     *         annotation is being used.
     */
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {

        if (navigationTarget != null && navigationTargetsInitialized()) {
            RouteTarget routeTarget = null;
            if (path != null) {
                routeTarget = routes.get().get(path);
            }
            Map<Class<? extends Component>, String> targetRoutesMap = targetRoutes
                    .get();
            if (routeTarget == null
                    && targetRoutesMap.containsKey(navigationTarget)) {
                String routePath = targetRoutesMap.get(navigationTarget);
                routeTarget = routes.get().get(routePath);
            }
            if (routeTarget != null) {
                ThemeDefinition theme = routeTarget
                        .getThemeFor(navigationTarget);
                if (theme != null) {
                    return Optional.of(theme);
                }
            }
        }
        return Optional.ofNullable(
                findThemeForNavigationTarget(navigationTarget, path));
    }

    public Class<?> getPwaConfigurationClass() {
        return pwaConfigurationClass;
    }

    protected void setPwaClass(Class<?> pwaClass) {
        if (pwaClass != null && pwaClass.isAnnotationPresent(PWA.class)) {
            this.pwaConfigurationClass = pwaClass;
        }
    }
}
