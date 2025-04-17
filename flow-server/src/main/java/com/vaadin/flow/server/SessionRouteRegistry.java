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
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ConfiguredRoutes;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.router.internal.RouteTarget;
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
            Set<String> collect = routes.stream().map(RouteData::getTemplate)
                    .collect(Collectors.toSet());
            registeredRoutes.stream()
                    .filter(data -> !collect.contains(data.getTemplate()))
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
                                    .hasTemplate(routeData.getTemplate()))
                            .collect(Collectors.toList());
                    List<RouteBaseData<?>> removedVisible = event
                            .getRemovedRoutes().stream()
                            .filter(routeData -> !configuration
                                    .hasTemplate(routeData.getTemplate()))
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
            RouteParameters parameters) {
        final RouteTarget routeTarget = getConfiguration()
                .getRouteTarget(target, parameters);
        if (routeTarget != null) {
            return routeTarget;
        }
        return getParentRegistry().getRouteTarget(target, parameters);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String url) {
        Objects.requireNonNull(url, "pathString must not be null.");
        final Optional<Class<? extends Component>> target = getConfiguration()
                .getTarget(url);
        if (target.isPresent() && !parentContainsExactMatch(url)) {
            return target;
        }

        return getParentRegistry().getNavigationTarget(url);
    }

    /**
     * When session registry contains a matching route, check if it is not an
     * exact match and parent registry contains an exact or closer match.
     *
     * @param url
     *            url to check for exact match
     * @return true if parent has exact match, but this registry doesn't
     */
    private boolean parentContainsExactMatch(String url) {
        final List<RouteData> parentRoutes = getParentRegistry()
                .getRegisteredRoutes();
        final List<RouteData> registeredRoutes = getRegisteredRoutes();

        // Remove any routes coming from parent registry
        registeredRoutes.removeAll(parentRoutes);

        boolean noLocalMatch = registeredRoutes.stream()
                .noneMatch(data -> data.getTemplate().equals(url));
        boolean parentMatch = parentRoutes.stream()
                .anyMatch(data -> data.getTemplate().equals(url));

        final boolean parentExactMatch = noLocalMatch && parentMatch;
        if (!parentExactMatch) {
            final List<String> segments = PathUtil.getSegmentsList(url);
            final int parentHighestMatch = parentRoutes.stream()
                    .mapToInt(data -> equalParts(segments,
                            PathUtil.getSegmentsList(data.getTemplate())))
                    .max().orElse(0);

            final int registryHighestMatch = registeredRoutes.stream()
                    .mapToInt(data -> equalParts(segments,
                            PathUtil.getSegmentsList(data.getTemplate())))
                    .max().orElse(0);
            return parentHighestMatch > registryHighestMatch;
        }
        return parentExactMatch;
    }

    /**
     * Check for how many parts match.
     *
     * @param urlParts
     *            url segment parts list
     * @param target
     *            route target segments list
     * @return amount of matching segments from the start
     */
    private int equalParts(List<String> urlParts, List<String> target) {
        int maxSize = Math.min(urlParts.size(), target.size());
        int matches = 0;
        for (int i = 0; i < maxSize; i++) {
            if (urlParts.get(i).equals(target.get(i))) {
                matches++;
            } else {
                break;
            }
        }
        return matches;
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(String url,
            List<String> segments) {
        Objects.requireNonNull(url, "url must not be null.");
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
            RouteParameters parameters) {
        Optional<String> targetUrl = super.getTargetUrl(navigationTarget,
                parameters);
        if (targetUrl.isPresent()) {
            return targetUrl;
        }

        return getParentRegistry().getTargetUrl(navigationTarget, parameters);
    }

    @Override
    public Optional<String> getTemplate(
            Class<? extends Component> navigationTarget) {
        final Optional<String> targetRoute = super.getTemplate(
                navigationTarget);
        if (targetRoute.isPresent()) {
            return targetRoute;
        }
        return getParentRegistry().getTemplate(navigationTarget);
    }

    @Override
    public VaadinContext getContext() {
        return session.getService().getContext();
    }

    private RouteRegistry getParentRegistry() {
        return session.getService().getRouteRegistry();
    }

    @Override
    public Class<? extends RouterLayout> getLayout(String path) {
        if (super.hasLayout(path)) {
            return super.getLayout(path);
        }
        return getParentRegistry().getLayout(path);
    }

    @Override
    public boolean hasLayout(String path) {
        if (super.hasLayout(path)) {
            return true;
        }
        return getParentRegistry().hasLayout(path);
    }
}
