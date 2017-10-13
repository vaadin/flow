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
package com.vaadin.server.startup;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.router.HasErrorParameter;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.InternalServerError;
import com.vaadin.router.Location;
import com.vaadin.router.NotFoundException;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouteNotFoundError;
import com.vaadin.router.RoutePrefix;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.util.AnnotationReader;
import com.vaadin.util.ReflectTools;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class RouteRegistry implements Serializable {

    private final Map<String, RouteTarget> routes = new HashMap<>();
    private final Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();
    private final Map<Class, Class<? extends Component>> exceptionTargets = new HashMap<Class, Class<? extends Component>>();

    private boolean initialized;
    private boolean errorTargetsInitialized;

    /**
     * Creates a new uninitialized route registry.
     */
    protected RouteRegistry() {
        initialized = false;
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

        Object attribute = servletContext
                .getAttribute(RouteRegistry.class.getName());

        if (attribute == null) {
            attribute = new RouteRegistry();
            servletContext.setAttribute(RouteRegistry.class.getName(),
                    attribute);
        }

        if (attribute instanceof RouteRegistry) {
            return (RouteRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private void clear() {
        routes.clear();
        targetRoutes.clear();
    }

    /**
     * Registers a set of components as navigation targets.
     * <p>
     * <strong>Note:</strong> Navigation targets can only be set once, i.e. when
     * {@link #isInitialized()} is {@code false}.
     *
     * @param navigationTargets
     *            set of navigation target components
     * @throws InvalidRouteConfigurationException
     *             if routing has been configured incorrectly
     */
    public void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        if (isInitialized()) {
            throw new InvalidRouteConfigurationException(
                    "Routes have already been initialized");
        }
        validateNavigationTargets(navigationTargets);
        doRegisterNavigationTargets(navigationTargets);
        initialized = true;
    }

    /**
     * Set error handler navigation targets.
     * 
     * @param errorNavigationTargets
     *            error handler navigation targets
     */
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {
        for (Class<? extends Component> target : errorNavigationTargets) {
            Class<?> exceptionType = ReflectTools
                    .getGenericInterfaceType(target, HasErrorParameter.class);

            if (exceptionTargets.containsKey(exceptionType)) {
                handleRegisteredExceptionType(target, exceptionType);
            } else {
                exceptionTargets.put(exceptionType, target);
            }
        }
        initErrorTargets();
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
            Class<? extends Component> target, Class<?> exceptionType) {
        Class<? extends Component> registered = exceptionTargets
                .get(exceptionType);

        if (registered.isAssignableFrom(target)) {
            exceptionTargets.put(exceptionType, target);
        } else if (!target.isAssignableFrom(registered)) {
            String msg = String.format(
                    "Only one target for an exception should be defined. Found '%s' and '%s' for exception '%s'",
                    target.getName(), registered.getName(),
                    exceptionType.getName());
            throw new InvalidRouteLayoutConfigurationException(msg);
        }
    }

    private void initErrorTargets() {
        if (!exceptionTargets.containsKey(NotFoundException.class)) {
            exceptionTargets.put(NotFoundException.class,
                    RouteNotFoundError.class);
        }
        if (!exceptionTargets.containsKey(Exception.class)) {
            exceptionTargets.put(Exception.class, InternalServerError.class);
        }
        errorTargetsInitialized = true;
    }

    /**
     * Get a registered navigation target for given exception. First we will
     * search for a matching cause for in the exception chain and if no match
     * found search by extended type.
     * 
     * @param exception
     *            exception to search error view for
     * @return optional error target corresponding to the given exception
     */
    public Optional<Class<? extends Component>> getErrorNavigationTarget(
            Throwable exception) {
        if (!errorTargetsInitialized) {
            initErrorTargets();
        }
        Class<? extends Component> result = searchByCause(exception);
        if (result == null) {
            result = searchBySuperType(exception);
        }
        return Optional.ofNullable(result);
    }

    private Class<? extends Component> searchByCause(Throwable exception) {
        if (exceptionTargets.containsKey(exception.getClass())) {
            return exceptionTargets.get(exception.getClass());
        }
        if (exception.getCause() != null) {
            return searchByCause(exception.getCause());
        }
        return null;
    }

    private Class<? extends Component> searchBySuperType(Throwable exception) {
        Class<?> superClass = exception.getClass().getSuperclass();
        do {
            if (exceptionTargets.containsKey(superClass)) {
                return exceptionTargets.get(superClass);
            }
            superClass = superClass.getSuperclass();
        } while (superClass != null
                && Throwable.class.isAssignableFrom(superClass));

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
            return Optional
                    .ofNullable(routes.get(pathString).getTarget(segments));
        }
        return Optional.empty();
    }

    /**
     * Checks if the registry contains a route to the given path.
     *
     * @param pathString
     *            path to get navigation target for, not {@code null}
     * @return true if the registry contains a route to the given path,
     *         false otherwise.
     */
    public boolean hasRouteTo(String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        return routes.containsKey(pathString);
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
        String route = targetRoutes.get(navigationTarget);
        if (HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            Class genericInterfaceType = ReflectTools.getGenericInterfaceType(
                    navigationTarget, HasUrlParameter.class);
            route = route + "/{" + genericInterfaceType.getSimpleName() + "}";
        }
        return route;
    }

    /**
     * Returns whether this registry has been initialized with navigation
     * targets.
     *
     * @return whether this registry has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    private void validateNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        Map<String, RouteTarget> navigationTargetMap = new HashMap<>();
        for (Class<? extends Component> navigationTarget : navigationTargets) {
            if (!navigationTarget.isAnnotationPresent(Route.class)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "No Route annotation is present for the given "
                                + "navigation target component '%s'.",
                        navigationTarget.getName()));
            }
            String route = getNavigationRoute(navigationTarget);
            targetRoutes.put(navigationTarget, route);
            // Further validation is performed inside addRoute()
            if (navigationTargetMap.containsKey(route)) {
                navigationTargetMap.get(route).addRoute(navigationTarget);
            } else {
                navigationTargetMap.put(route,
                        new RouteTarget(navigationTarget));
            }
        }
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
    private String getNavigationRoute(Class<?> navigationTarget) {
        Route annotation = navigationTarget.getAnnotation(Route.class);
        if (annotation.absolute()) {
            return annotation.value();
        }

        List<String> parentRoutePrefixes = getParentRoutePrefixes(
                navigationTarget);
        Collections.reverse(parentRoutePrefixes);
        if (!annotation.value().isEmpty()) {
            parentRoutePrefixes.add(annotation.value());
        }

        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    private List<String> getParentRoutePrefixes(Class<?> component) {
        List<String> list = new ArrayList<>();

        Optional<Route> router = AnnotationReader.getAnnotationFor(component,
                Route.class);
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);
        Optional<RoutePrefix> routePrefix = AnnotationReader
                .getAnnotationFor(component, RoutePrefix.class);

        routePrefix.ifPresent(prefix -> list.add(prefix.value()));

        // break chain on an absolute RoutePrefix or Route
        if ((routePrefix.isPresent() && routePrefix.get().absolute())
                || (router.isPresent() && router.get().absolute())) {
            return list;
        }

        if (router.isPresent() && !router.get().layout().equals(UI.class)) {
            list.addAll(getParentRoutePrefixes(router.get().layout()));
        } else if (parentLayout.isPresent()) {
            list.addAll(getParentRoutePrefixes(parentLayout.get().value()));
        }

        return list;
    }

    private void doRegisterNavigationTargets(
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {
        Logger logger = Logger.getLogger(RouteRegistry.class.getName());

        clear();
        for (Class<? extends Component> navigationTarget : navigationTargets) {
            String route = getNavigationRoute(navigationTarget);
            targetRoutes.put(navigationTarget, route);
            if (routes.containsKey(route)) {
                routes.get(route).addRoute(navigationTarget);
            } else {
                String message = String.format(
                        "Registering route '%s' to navigation target '%s'.",
                        route, navigationTarget.getName());
                logger.log(Level.FINE, message);

                routes.put(route, new RouteTarget(navigationTarget));
            }
        }
    }

    /**
     * Checks whether any navigation targets have been registered.
     *
     * @return <code>true</code> if at least one navigation target is
     *         registered; otherwise <code>false</code>
     */
    public boolean hasNavigationTargets() {
        return !routes.isEmpty();
    }
}
