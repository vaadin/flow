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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.RouteTarget;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * SessionRouteRegistry is a mutable route registry that is valid in the scope
 * of VaadinSession. Routes can be added and removed from this registry and any
 * overlap with the registered routes between session and global scope will be
 * handled so that session scope paths overrides global paths.
 */
public class SessionRouteRegistry implements RouteRegistry {

    private RouteRegistry globalRegistry;

    private final Map<String, RouteTarget> routes = new HashMap<>();
    private final Map<Class<? extends Component>, String> targetRoutes = new HashMap<>();

    private final AtomicReference<Map<Class<? extends Exception>, Class<? extends Component>>> exceptionTargets = new AtomicReference<>();
    private final AtomicReference<List<RouteData>> routeData = new AtomicReference<>();
    private final Registration registration;
    private final VaadinSession session;

    private SessionRouteRegistry(VaadinSession session) {
        this.session = session;

        registration = VaadinService.getCurrent()
                .addSessionDestroyListener(this::sessionDestroy);
    }

    private void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
        if (sessionDestroyEvent.getSession().equals(session)) {
            registration.remove();
            clear();
        }
    }

    /**
     * Clear all registered routes from this SessionRouteRegistry.
     */
    public void clear() {
        routes.clear();
        targetRoutes.clear();
    }

    public static SessionRouteRegistry getSessionRegistry() {
        return getSessionRegistry(VaadinSession.getCurrent());
    }

    public static SessionRouteRegistry getSessionRegistry(
            VaadinSession session) {
        Objects.requireNonNull(session,
                "Null session is not supported for session route registry");
        SessionRouteRegistry registry = session
                .getAttribute(SessionRouteRegistry.class);
        if (registry == null) {
            registry = new SessionRouteRegistry(session);
            session.setAttribute(SessionRouteRegistry.class, registry);
        }
        return registry;
    }

    public static boolean sessionRegistryExists(VaadinSession session) {
        return session.getAttribute(SessionRouteRegistry.class) != null;
    }

    @Override
    public void setNavigationTargets(
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
                    .format("Classes [%s] given as navigation targets were not valid. "
                                    + "Use SessionRouteRegistry method "
                                    + "setRoute(String ,Class<? extends Component>, List<Class<? extends RouterLayout>>) instead.",
                            faultyClasses.toString());
            throw new InvalidRouteConfigurationException(exceptionMessage);
        }

        Map<String, RouteTarget> routesMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutesMap = new HashMap<>();

        for (Class<? extends Component> navigationTarget : navigationTargets) {
            Set<String> routeAndRouteAliasPaths = new HashSet<>();

            String route = RouterUtil
                    .getNavigationRouteAndAliases(navigationTarget,
                            routeAndRouteAliasPaths);
            routeAndRouteAliasPaths.add(route);

            targetRoutesMap.put(navigationTarget, route);

            for (String path : routeAndRouteAliasPaths) {
                RouteTarget routeTarget;
                if (routesMap.containsKey(path)) {
                    routeTarget = routesMap.get(path);
                    routeTarget.addRoute(navigationTarget);
                } else {
                    routeTarget = new RouteTarget(navigationTarget);
                    routesMap.put(path, routeTarget);
                }
                routeTarget.setThemeFor(navigationTarget, RouterUtil
                        .findThemeForNavigationTarget(navigationTarget, path));
            }
        }
        synchronized (routes) {
            routes.putAll(routesMap);
            targetRoutes.putAll(targetRoutesMap);
        }
    }

    /**
     * Giving a navigation target here will handle the {@link Route} annotation
     * to get
     * the path and also register any {@link RouteAlias} that may be on the
     * class.
     *
     * @param navigationTarget
     *         navigation target to register into the session route scope
     * @throws InvalidRouteConfigurationException
     *         thrown if exact route already defined in this scope
     */
    public void setRoute(Class<? extends Component> navigationTarget)
            throws InvalidRouteConfigurationException {
        Set<String> routeAndRouteAliasPaths = new HashSet<>();

        String route = RouterUtil.getNavigationRouteAndAliases(navigationTarget,
                routeAndRouteAliasPaths);
        routeAndRouteAliasPaths.add(route);

        Map<String, RouteTarget> routesMap = new HashMap<>();
        Map<Class<? extends Component>, String> targetRoutesMap = new HashMap<>();

        for (String path : routeAndRouteAliasPaths) {
            RouteTarget routeTarget;
            if (routesMap.containsKey(path)) {
                routeTarget = routesMap.get(path);
                routeTarget.addRoute(navigationTarget);
            } else {
                routeTarget = new RouteTarget(navigationTarget);
                routesMap.put(path, routeTarget);
            }

        }
        targetRoutesMap.put(navigationTarget, route);

        synchronized (routes) {
            routes.putAll(routesMap);
            targetRoutes.putAll(targetRoutesMap);
        }

    }

    /**
     * Regiser a navigation target on the specified path. Any {@link
     * ParentLayout} annotation on class will be used to populate layout chain,
     * but {@link Route} and {@link RouteAlias} should not be present on class.
     * (as for this implementation version)
     *
     * @param path
     *         path to register navigation target to
     * @param navigationTarget
     *         navigation target to register into the session route scope
     * @throws InvalidRouteConfigurationException
     *         thrown if exact route already defined in this scope
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget)
            throws InvalidRouteConfigurationException {
        synchronized (routes) {
            RouteTarget routeTarget;
            if (routes.containsKey(path)) {
                routeTarget = routes.get(path);
                routeTarget.addRoute(navigationTarget);
            } else {
                routeTarget = new RouteTarget(navigationTarget);
                routes.put(path, routeTarget);
            }

            targetRoutes.put(navigationTarget, path);
        }
    }

    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            Iterable<Class<? extends RouterLayout>> parentChain)
            throws InvalidRouteConfigurationException {
        setRoute(path, navigationTarget);
    }

    @Override
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {

    }

    @Override
    public List<RouteData> getRegisteredRoutes() {
        return null;
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        Optional<Class<? extends Component>> navigationTarget = getNavigationTarget(
                pathString, new ArrayList<>());

        if (navigationTarget.isPresent()) {
            return navigationTarget;
        }
        return globalRegistry.getNavigationTarget(pathString);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        if (routes.containsKey(pathString)) {
            Optional<Class<? extends Component>> target = Optional
                    .ofNullable(routes.get(pathString).getTarget(segments));
            if (target.isPresent()) {
                return target;
            }
        }
        return globalRegistry.getNavigationTarget(pathString, segments);
    }

    @Override
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        return globalRegistry.getErrorNavigationTarget(exception);
    }

    @Override
    public boolean hasRouteTo(String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        return routes.containsKey(pathString) || globalRegistry
                .hasRouteTo(pathString);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        return Optional.empty();
    }

    @Override
    public boolean hasNavigationTargets() {
        return false;
    }

    @Override
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        return Optional.empty();
    }

    public SessionRouteRegistry withGlobalRegistry(
            RouteRegistry globalRegistry) {
        if (this.globalRegistry == null)
            this.globalRegistry = globalRegistry;
        else if (!this.globalRegistry.equals(globalRegistry))
            throw new RuntimeException(
                    "Session registry got a new GlobalRouteRegistry which should not be possible!");
        return this;
    }
}
