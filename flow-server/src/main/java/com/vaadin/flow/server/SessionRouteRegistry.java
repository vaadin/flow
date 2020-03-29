/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameterFormat;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.UrlParameters;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ConfiguredRoutes;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.server.startup.RouteTarget;
import com.vaadin.flow.shared.Registration;

/**
 * SessionRouteRegistry is a mutable route registry that is valid in the scope
 * of VaadinSession. Routes can be added and removed from this registry and any
 * overlap with the registered routes between session and global scope will be
 * handled so that session scope paths overrides global paths.
 *
 * @since 1.3
 */
public class SessionRouteRegistry extends AbstractRouteRegistry {

    private final VaadinSession session;

    /**
     * Package protected constructor for the session route registry.
     * <p>
     * This is only applicable for VaadinSession as no other registry will be
     * taken into consideration.
     *
     * @param session
     *            vaadin session that this session registry is
     */
    SessionRouteRegistry(VaadinSession session) {
        this.session = session;
    }

    /**
     * Get the session registry for VaadinSession. If no SessionRegistry exists
     * then one will be created for given VaadinSession.
     *
     * @param session
     *            vaadin session to get registry for
     * @return session registry for given session
     */
    public static RouteRegistry getSessionRegistry(VaadinSession session) {
        Objects.requireNonNull(session,
                "Null session is not supported for session route registry");
        SessionRouteRegistry registry = session
                .getAttribute(SessionRouteRegistry.class);
        if (registry == null) {
            registry = new SessionRouteRegistry(session);
            session.setAttribute(SessionRouteRegistry.class, registry);
        }
        if (!registry.session.equals(session)) {
            throw new IllegalStateException(
                    "Session has as the attribute a route registered to another session");
        }
        return registry;
    }

    @Override
    public List<RouteData> getRegisteredRoutes() {
        List<RouteData> routes = new ArrayList<>(super.getRegisteredRoutes());

        List<RouteData> registeredRoutes = getParentRegistry()
                .getRegisteredRoutes();
        if (!registeredRoutes.isEmpty()) {
            Set<String> collect = routes.stream().map(RouteData::getUrlTemplate)
                    .collect(Collectors.toSet());
            registeredRoutes.stream()
                    .filter(data -> !collect.contains(data.getUrlTemplate()))
                    .forEach(routes::add);
        }

        return routes;
    }

    /**
     * Adds the given route change listener to the registry.
     * <p>
     * For the session scoped registry also changes to the application scoped
     * registry will be delegated to the listener if the added or removed route
     * was not masked by a registration in the session scope.
     *
     * @param listener
     *            listener to add
     * @return registration to remove the listener
     */
    @Override
    public Registration addRoutesChangeListener(
            RoutesChangedListener listener) {

        final Registration parentRegistration = getParentRegistry()
                .addRoutesChangeListener(event -> {
                    ConfiguredRoutes configuration = getConfiguration();
                    List<RouteBaseData<?>> addedVisible = event.getAddedRoutes()
                            .stream()
                            .filter(routeData -> !configuration
                                    .hasUrlTemplate(routeData.getUrlTemplate()))
                            .collect(Collectors.toList());
                    List<RouteBaseData<?>> removedVisible = event
                            .getRemovedRoutes().stream()
                            .filter(routeData -> !configuration
                                    .hasUrlTemplate(routeData.getUrlTemplate()))
                            .collect(Collectors.toList());
                    // Only fire an event if we have visible changes.
                    if (!(addedVisible.isEmpty() && removedVisible.isEmpty())) {
                        fireEvent(new RoutesChangedEvent(event.getSource(),
                                addedVisible, removedVisible));
                    }
                });
        final Registration registration = super.addRoutesChangeListener(
                listener);

        return () -> {
            registration.remove();
            parentRegistration.remove();
        };
    }

    @Override
    public NavigationRouteTarget getNavigationRouteTarget(String url) {
        final NavigationRouteTarget navigationRouteTarget = getConfiguration()
                .getNavigationRouteTarget(url);
        if (navigationRouteTarget.hasTarget()) {
            return navigationRouteTarget;
        }
        return getParentRegistry().getNavigationRouteTarget(url);
    }

    @Override
    public RouteTarget getRouteTarget(Class<? extends Component> target,
                                      UrlParameters parameters) {
        final RouteTarget routeTarget = getConfiguration()
                .getRouteTarget(target, parameters);
        if (routeTarget != null) {
            return routeTarget;
        }
        return getParentRegistry().getRouteTarget(target, parameters);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        final Optional<Class<? extends Component>> target = getConfiguration()
                .getTarget(pathString);
        if (target.isPresent()) {
            return target;
        }

        return getParentRegistry().getNavigationTarget(pathString);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String url, List<String> segments) {
        Objects.requireNonNull(url, "pathString must not be null.");
        final Optional<Class<? extends Component>> target = getConfiguration()
                .getTarget(PathUtil.getPath(url, segments));
        if (target.isPresent()) {
            return target;
        }

        return getParentRegistry().getNavigationTarget(url, segments);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        Optional<String> targetUrl = super.getTargetUrl(navigationTarget);
        if (targetUrl.isPresent()) {
            return targetUrl;
        }
        return getParentRegistry().getTargetUrl(navigationTarget);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget,
            UrlParameters parameters) {
        Optional<String> targetUrl = super.getTargetUrl(navigationTarget,
                parameters);
        if (targetUrl.isPresent()) {
            return targetUrl;
        }

        return getParentRegistry().getTargetUrl(navigationTarget, parameters);
    }

    @Override
    public Optional<String> getUrlTemplate(
            Class<? extends Component> navigationTarget) {
        final Optional<String> targetRoute = super.getUrlTemplate(
                navigationTarget);
        if (targetRoute.isPresent()) {
            return targetRoute;
        }
        return getParentRegistry().getUrlTemplate(navigationTarget);
    }

    @Override
    public Optional<String> getUrlTemplate(
            Class<? extends Component> navigationTarget,
            Set<RouteParameterFormat> format) {
        final Optional<String> targetRoute = super.getUrlTemplate(
                navigationTarget, format);
        if (targetRoute.isPresent()) {
            return targetRoute;
        }
        return getParentRegistry().getUrlTemplate(navigationTarget, format);
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(String url,
            Class<? extends Component> navigationTarget) {
        final NavigationRouteTarget navigationRouteTarget = getConfiguration()
                .getNavigationRouteTarget(url);
        if (navigationRouteTarget.hasTarget()) {
            return navigationRouteTarget.getRouteTarget().getParentLayouts();
        }
        return getParentRegistry().getRouteLayouts(url, navigationTarget);
    }

    private RouteRegistry getParentRegistry() {
        return session.getService().getRouteRegistry();
    }
}
