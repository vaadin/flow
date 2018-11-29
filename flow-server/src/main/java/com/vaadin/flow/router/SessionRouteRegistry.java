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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteConfiguration;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.RouteTarget;
import com.vaadin.flow.shared.Registration;

/**
 * SessionRouteRegistry is a mutable route registry that is valid in the scope
 * of VaadinSession. Routes can be added and removed from this registry and any
 * overlap with the registered routes between session and global scope will be
 * handled so that session scope paths overrides global paths.
 */
public class SessionRouteRegistry extends AbstractRouteRegistry {

    // SessionRegistry can not be used without a parentRegistry
    private RouteRegistry parentRegistry;

    private static class SessionConfiguration extends RouteConfiguration {
        private final Map<String, List<Class<? extends RouterLayout>>> manualLayouts = new HashMap<>();

        public SessionConfiguration(RouteConfiguration configuration,
                boolean mutable) {
            super(configuration, mutable);
            if (configuration instanceof SessionConfiguration) {
                manualLayouts
                        .putAll(((SessionConfiguration) configuration).manualLayouts);
            }
        }

        @Override
        public void clear() {
            super.clear();
            manualLayouts.clear();
        }
    }

    private final Registration registration;
    private final VaadinSession session;

    /**
     * Override the RouteConfiguration to use the SessionConfiguration instead
     * as we have some extra information that we want to store.
     *
     * @param original
     *         the latest route configuration
     * @param mutable
     *         set the configuration as mutable or immutable
     * @return extended RouteConfiguration.
     */
    @Override
    protected RouteConfiguration getRouteConfiguration(
            RouteConfiguration original, boolean mutable) {
        return new SessionConfiguration(original, mutable);
    }

    private SessionRouteRegistry(VaadinSession session) {
        this.session = session;

        registration = session.getService()
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
        configure((configuration) -> {
            configuration.clear();
        });
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

        configure(configuration -> {
            for (Class<? extends Component> navigationTarget : navigationTargets) {
                setRoute(navigationTarget, configuration);
            }
        });
    }

    @Override
    public void setRoute(Class<? extends Component> navigationTarget)
            throws InvalidRouteConfigurationException {
        configure(configuration -> setRoute(navigationTarget, configuration));
    }

    /**
     * Register a navigation target on the specified path. Any {@link
     * ParentLayout} annotation on class will be used to populate layout chain,
     * but {@link Route} and {@link RouteAlias} will not be taken into
     * consideration.
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
        configure(configuration -> {
            addRouteToConfiguration(path, navigationTarget, configuration);
        });
    }

    /**
     * Register a navigation target with specified path and given parent layout
     * chain.
     * Any {@link ParentLayout}, {@link Route} or {@link RouteAlias} will be
     * ignored in route handling.
     *
     * @param path
     *         path to register navigation target to
     * @param navigationTarget
     *         navigation target to register into session scope
     * @param parentChain
     *         chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *         thrown if exact route already defined in this scope
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain)
            throws InvalidRouteConfigurationException {
        configure(configuration -> {
            addRouteToConfiguration(path, navigationTarget, configuration);
            ((SessionConfiguration) configuration).manualLayouts
                    .put(path, Collections.unmodifiableList(parentChain));
        });
    }

    /**
     * This adds a new route path to the configuration.
     * <p>
     * Note! this should only be called from a configure() for thread safety.
     *
     * @param path
     *         path for the navigation target
     * @param navigationTarget
     *         navigation target for given path
     * @param configuration
     *         mutable configuration object
     */
    private void addRouteToConfiguration(String path,
            Class<? extends Component> navigationTarget,
            RouteConfiguration configuration) {
        if (configuration.hasRoute(path)) {
            configuration.addRouteTarget(path, navigationTarget);
        } else {
            RouteTarget routeTarget = new RouteTarget(navigationTarget);
            configuration.setRouteTarget(path, routeTarget);
        }

        configuration.putTargetRoute(navigationTarget, path);
    }

    public void removeRoute(String path) {

    }

    public void removeRoute(Class<? extends Component> routeTarget) {

    }

    @Override
    public void setErrorNavigationTargets(
            Set<Class<? extends Component>> errorNavigationTargets) {

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
        return parentRegistry.getNavigationTarget(pathString);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {
        Objects.requireNonNull(pathString, "pathString must not be null.");
        if (routeConfiguration.hasRoute(pathString, segments)) {
            return routeConfiguration.getRoute(pathString, segments);
        }
        return parentRegistry.getNavigationTarget(pathString, segments);
    }

    @Override
    public Optional<ErrorTargetEntry> getErrorNavigationTarget(
            Exception exception) {
        // TODO:
        return parentRegistry.getErrorNavigationTarget(exception);
    }

    @Override
    public boolean hasRouteTo(String pathString) {
        Objects.requireNonNull(pathString, "pathString must not be null.");

        return routeConfiguration.hasRoute(pathString) || parentRegistry
                .hasRouteTo(pathString);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {

        return parentRegistry.getTargetUrl(navigationTarget);
    }

    @Override
    public boolean hasNavigationTargets() {
        return !routeConfiguration.isEmpty() || parentRegistry
                .hasNavigationTargets();
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(
            Class<? extends Component> navigationTarget, String path) {
        if (routeConfiguration.hasRoute(path) && (
                !navigationTarget.isAnnotationPresent(Route.class)
                        || ((SessionConfiguration) routeConfiguration).manualLayouts
                        .containsKey(path))) {

            // User has defined parent layouts manually use those
            if (((SessionConfiguration) routeConfiguration).manualLayouts
                    .containsKey(path)) {
                return ((SessionConfiguration) routeConfiguration).manualLayouts
                        .get(path);
            }
            // not a route layout use non route layout collection of parent layouts.
            return getNonRouteLayouts(navigationTarget);
        }
        return parentRegistry.getRouteLayouts(navigationTarget, path);
    }

    public SessionRouteRegistry withParentRegistry(
            RouteRegistry routeRegistry) {
        if (this.parentRegistry == null)
            this.parentRegistry = routeRegistry;
        else if (!this.parentRegistry.equals(routeRegistry))
            throw new RuntimeException(
                    "Session registry got a new GlobalRouteRegistry which should not be possible!");
        return this;
    }
}
