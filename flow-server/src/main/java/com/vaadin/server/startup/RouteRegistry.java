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

import javax.servlet.ServletContext;

import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.Location;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RoutePrefix;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.util.AnnotationReader;
import com.vaadin.util.ReflectTools;

/**
 * Registry for holding navigation target components found on servlet
 * initialization.
 */
public class RouteRegistry implements Serializable {

    private final Map<String, Class<? extends Component>> routes = new HashMap<>();
    private final Map<String, Class<? extends Component>> parameterRoutes = new HashMap<>();
    private final Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();

    private boolean initialized;

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
        parameterRoutes.clear();
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
        return Optional.ofNullable(routes.get(pathString));
    }

    /**
     * Gets the optional navigation target class for a given Location. Returns
     * an empty optional if no navigation target corresponds to the given
     * Location.
     * <p>
     * In cases where we have a parametrized and non parametrized navigation
     * target for the given location the parametrized version is returned.
     *
     * @see Location
     *
     * @param pathString
     *            the path to get the navigation target for, not {@code null}
     * @return optional of the navigation target corresponding to the given
     *         location with precedence to parametrized navigation target
     */
    public Optional<Class<? extends Component>> getNavigationTargetWithParameter(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        if (parameterRoutes.containsKey(pathString)) {
            return Optional.of(parameterRoutes.get(pathString));
        }
        return Optional.ofNullable(routes.get(pathString));
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
        Map<String, Class<? extends Component>> navigationTargetMap = new HashMap<>();
        for (Class<? extends Component> navigationTarget : navigationTargets) {
            if (!navigationTarget.isAnnotationPresent(Route.class)) {
                throw new InvalidRouteConfigurationException(String.format(
                        "No Route annotation is present for the given "
                                + "navigation target component '%s'.",
                        navigationTarget.getName()));
            }
            String route = getNavigationRoute(navigationTarget);
            targetRoutes.put(navigationTarget, route);
            if (navigationTargetMap.containsKey(route)) {
                checkForInvalidConfiguration(navigationTargetMap.get(route),
                        navigationTarget, route);
            }
            navigationTargetMap.put(route, navigationTarget);
        }
    }

    /**
     * Check that the targets that define the same route are actually able to do
     * so.
     * <p>
     * The same route targets need to have:
     * <p>
     * Clearly different end URLs which means that one has a required url
     * parameter. so they then match for the URLs `route` and `route/{param}`
     *
     * @param existingTarget
     *            already mapped navigation target for route
     * @param newTarget
     *            new navigation target for route
     * @param route
     *            route
     * @throws InvalidRouteConfigurationException
     *             in cases where the configuration is wrong
     */
    private void checkForInvalidConfiguration(
            Class<? extends Component> existingTarget,
            Class<? extends Component> newTarget, String route)
            throws InvalidRouteConfigurationException {

        // neither route has parameters
        if (route.equals(collectRequiredParameters(newTarget))
                && route.equals(collectRequiredParameters(existingTarget))) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets must have unique routes, "
                            + "found navigation targets '%s' and '%s' with the same route.",
                    existingTarget.getName(), newTarget.getName()));
            // Found parameter is optional
        } else if (checkIfOptionalParameter(newTarget, existingTarget)) {
            String optional = HasUrlParameter.isOptionalParameter(newTarget)
                    ? newTarget.getName() : existingTarget.getName();

            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                    existingTarget.getName(), newTarget.getName(), optional));
        }
    }

    private boolean checkIfOptionalParameter(
            Class<? extends Component> navigationTarget,
            Class<? extends Component> previousTarget) {
        return HasUrlParameter.isOptionalParameter(navigationTarget)
                || HasUrlParameter.isOptionalParameter(previousTarget);
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

        StringBuilder fullRoute = new StringBuilder();
        List<String> parentRoutePrefixes = getParentRoutePrefixes(
                navigationTarget);
        Collections.reverse(parentRoutePrefixes);

        parentRoutePrefixes
                .forEach(prefix -> fullRoute.append(prefix).append("/"));

        fullRoute.append(annotation.value());

        return fullRoute.toString();
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
            Set<Class<? extends Component>> navigationTargets) {
        Logger logger = Logger.getLogger(RouteRegistry.class.getName());

        clear();
        for (Class<? extends Component> navigationTarget : navigationTargets) {
            String route = getNavigationRoute(navigationTarget);
            targetRoutes.put(navigationTarget, route);
            if (routes.containsKey(route)) {
                if (!route
                        .equals(collectRequiredParameters(navigationTarget))) {
                    String message = String.format(
                            "Registering route '%s' also to parametrized navigation target '%s'.",
                            route, navigationTarget.getName());
                    logger.log(Level.FINE, message);

                    parameterRoutes.put(route, navigationTarget);
                } else if (!route
                        .equals(collectRequiredParameters(routes.get(route)))) {
                    String message = String.format(
                            "Registering '%s' to route '%s' together with parametrized navigation target '%s'.",
                            navigationTarget.getName(), route,
                            routes.get(route).getName());
                    logger.log(Level.FINE, message);

                    parameterRoutes.put(route, routes.get(route));
                    routes.put(route, navigationTarget);
                }
            } else {
                String message = String.format(
                        "Registering route '%s' to navigation target '%s'.",
                        route, navigationTarget.getName());
                logger.log(Level.FINE, message);

                routes.put(route, navigationTarget);
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
