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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteAliasData;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.shared.Registration;

/**
 * AbstractRouteRegistry with locking support and configuration.
 *
 * @since 1.3
 */
public abstract class AbstractRouteRegistry implements RouteRegistry {

    /**
     * Configuration interface to use for updating the configuration entity.
     */
    @FunctionalInterface
    public interface Configuration extends Serializable {
        /**
         * Configure the given configurable route holder object.
         *
         * @param configuration
         *            mutable route configuration to make changes to
         */
        void configure(ConfigureRoutes configuration);
    }

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    /**
     * The live configuration for this route registry. This can only be updated
     * through {@link #configure(Configuration)} for concurrency reasons.
     */
    private volatile ConfiguredRoutes configuredRoutes = new ConfiguredRoutes();
    private volatile ConfigureRoutes editing = null;

    private CopyOnWriteArrayList<RoutesChangedListener> routesChangedListeners = new CopyOnWriteArrayList<>();

    /**
     * Thread-safe update of the RouteConfiguration.
     *
     * @param command
     *            command that will mutate the configuration copy.
     */
    protected void configure(Configuration command) {
        lock();
        try {
            if (editing == null) {
                editing = new ConfigureRoutes(configuredRoutes);
            }

            command.configure(editing);

        } finally {
            unlock();
        }
    }

    @Override
    public void update(Command command) {
        lock();
        try {
            command.execute();
        } finally {
            unlock();
        }
    }

    private void lock() {
        configurationLock.lock();
    }

    private void unlock() {
        if (configurationLock.getHoldCount() == 1 && editing != null) {
            try {
                ConfiguredRoutes oldConfiguration = configuredRoutes;

                configuredRoutes = new ConfiguredRoutes(editing);

                if (!routesChangedListeners.isEmpty()) {
                    List<RouteBaseData<?>> oldRoutes = flattenRoutes(
                            getRegisteredRoutes(oldConfiguration));
                    List<RouteBaseData<?>> newRoutes = flattenRoutes(
                            getRegisteredRoutes(configuredRoutes));
                    List<RouteBaseData<?>> added = new ArrayList<>();
                    List<RouteBaseData<?>> removed = new ArrayList<>();

                    oldRoutes.stream()
                            .filter(route -> !newRoutes.contains(route))
                            .forEach(removed::add);
                    newRoutes.stream()
                            .filter(route -> !oldRoutes.contains(route))
                            .forEach(added::add);

                    fireEvent(new RoutesChangedEvent(this, added, removed));
                }
            } finally {
                editing = null;
                configurationLock.unlock();
            }
        } else {
            configurationLock.unlock();
        }
    }

    /**
     * Fire routes changed event to all registered listeners.
     *
     * @param routeChangedEvent
     *            event containing changes
     */
    protected void fireEvent(RoutesChangedEvent routeChangedEvent) {
        routesChangedListeners
                .forEach(listener -> listener.routesChanged(routeChangedEvent));
    }

    @Override
    public Registration addRoutesChangeListener(
            RoutesChangedListener listener) {
        routesChangedListeners.add(listener);
        return () -> routesChangedListeners.remove(listener);
    }

    protected boolean hasLock() {
        return configurationLock.isHeldByCurrentThread();
    }

    /**
     * Get the current valid configuration.
     * <p>
     * Note! there may exist a possibility that someone updates this while it's
     * being read, but the given configuration is valid at the given point in
     * time.
     *
     * @return current state of the registry as a value object
     */
    public ConfiguredRoutes getConfiguration() {
        if (configurationLock.isHeldByCurrentThread() && editing != null) {
            return editing;
        }
        return configuredRoutes;
    }

    @Override
    public List<RouteData> getRegisteredRoutes() {
        return getRegisteredRoutes(getConfiguration());
    }

    private List<RouteData> getRegisteredRoutes(
            ConfiguredRoutes configuration) {
        List<RouteData> registeredRoutes = new ArrayList<>();
        configuration.getTargetRoutes().forEach((target, url) -> {
            List<Class<?>> parameters = getRouteParameters(target);

            List<RouteAliasData> routeAliases = new ArrayList<>();

            configuration.getRoutePaths(target).stream()
                    .filter(route -> !route.equals(url))
                    .forEach(route -> routeAliases.add(new RouteAliasData(
                            getParentLayouts(configuration, target, route),
                            route, parameters, target)));
            List<Class<? extends RouterLayout>> parentLayouts = getParentLayouts(
                    configuration, target, url);
            RouteData route = new RouteData(parentLayouts, url, parameters,
                    target, routeAliases);
            registeredRoutes.add(route);
        });

        Collections.sort(registeredRoutes);

        return Collections.unmodifiableList(registeredRoutes);
    }

    /**
     * Flatten route data so that all route aliases are also as their own
     * entries in the list. Removes any route aliases as the route is the same
     * even if aliases change.
     *
     * @param routeData
     *            route data to flatten.
     * @return flattened list of routes and aliases
     */
    private List<RouteBaseData<?>> flattenRoutes(List<RouteData> routeData) {
        List<RouteBaseData<?>> flatRoutes = new ArrayList<>();
        for (RouteData route : routeData) {
            RouteData nonAliasCollection = new RouteData(
                    route.getParentLayouts(), route.getUrl(),
                    route.getParameters(), route.getNavigationTarget(),
                    Collections.emptyList());

            flatRoutes.add(nonAliasCollection);
            route.getRouteAliases().forEach(flatRoutes::add);
        }

        return flatRoutes;
    }

    private List<Class<? extends RouterLayout>> getParentLayouts(
            ConfiguredRoutes configuration, Class<? extends Component> target,
            String url) {
        RouteTarget routeTarget = configuration.getRouteTarget(url);
        if (routeTarget != null) {
            return routeTarget.getParentLayouts(target);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(String path,
            Class<? extends Component> navigationTarget) {
        if (getConfiguration().hasRoute(path)) {
            return getConfiguration().getParentLayouts(path, navigationTarget);
        }
        return Collections.emptyList();
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

    @Override
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
        if (!getConfiguration().hasRouteTarget(navigationTarget)) {
            return null;
        }
        StringBuilder route = new StringBuilder(
                getConfiguration().getTargetRoute(navigationTarget));

        List<Class<?>> routeParameters = getRouteParameters(navigationTarget);

        if (!routeParameters.isEmpty()) {
            routeParameters.forEach(param -> route.append("/{")
                    .append(param.getSimpleName()).append("}"));
        }
        return route.toString();
    }

    @Override
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        configure(configuration -> {
            RouteTarget routeTarget = addRouteToConfiguration(path,
                    navigationTarget, configuration);
            routeTarget.setParentLayouts(navigationTarget, parentChain);
        });
    }

    @Override
    public void removeRoute(Class<? extends Component> routeTarget) {
        if (!getConfiguration().hasRouteTarget(routeTarget)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(routeTarget));
    }

    @Override
    public void removeRoute(String path) {
        if (!getConfiguration().hasRoute(path)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(path));
    }

    @Override
    public void removeRoute(String path,
            Class<? extends Component> navigationTarget) {
        if (!getConfiguration().hasRoute(path)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(path,
                navigationTarget));
    }

    @Override
    public void clean() {
        configure(ConfigureRoutes::clear);
    }

    /**
     * This adds a new route path to the configuration.
     * <p>
     * Note! this should only be called from a configure() for thread safety.
     *
     * @param path
     *            path for the navigation target
     * @param navigationTarget
     *            navigation target for given path
     * @param configuration
     *            mutable configuration object
     * @return the route target to which the target was added
     */
    private RouteTarget addRouteToConfiguration(String path,
            Class<? extends Component> navigationTarget,
            ConfigureRoutes configuration) {
        if (!hasLock()) {
            throw new IllegalStateException(
                    "addRouteToConfiguration requires the registry lock and a mutable configuration.");
        }

        configuration.setRoute(path, navigationTarget);

        if (!configuration.hasRouteTarget(navigationTarget)) {
            configuration.setTargetRoute(navigationTarget, path);
        }
        return configuration.getRouteTarget(path);
    }

    /**
     * Add the given error target to the exceptionTargetMap. This will handle
     * existing overlapping exception types by assigning the correct error
     * target according to inheritance or throw if existing and new are not
     * related.
     *
     * @param target
     *            error handler target
     * @param exceptionTargetsMap
     *            map of existing error handlers
     * @throws InvalidRouteConfigurationException
     *             if trying to add a non related exception handler for which a
     *             handler already exists
     */
    protected void addErrorTarget(Class<? extends Component> target,
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

    /**
     * Get the exception handler for given exception or recurse by exception
     * cause until possible exception with handler found.
     *
     * @param exception
     *            exception to get handler for
     * @return Optional containing found handler or empty if none found
     */
    protected Optional<ErrorTargetEntry> searchByCause(Exception exception) {
        Class<? extends Component> targetClass = getConfiguration()
                .getExceptionHandlerByClass(exception.getClass());

        if (targetClass != null) {
            return Optional.of(
                    new ErrorTargetEntry(targetClass, exception.getClass()));
        }

        Throwable cause = exception.getCause();
        if (cause instanceof Exception) {
            return searchByCause((Exception) cause);
        }
        return Optional.empty();
    }

    /**
     * Search given exception super classes to get exception handler for if any
     * exist.
     *
     * @param exception
     *            exception to get handler for
     * @return Optional containing found handler or empty if none found
     */
    protected Optional<ErrorTargetEntry> searchBySuperType(
            Throwable exception) {
        Class<?> superClass = exception.getClass().getSuperclass();
        while (superClass != null
                && Exception.class.isAssignableFrom(superClass)) {
            Class<? extends Component> targetClass = getConfiguration()
                    .getExceptionHandlerByClass(superClass);
            if (targetClass != null) {
                return Optional.of(new ErrorTargetEntry(targetClass,
                        superClass.asSubclass(Exception.class)));
            }
            superClass = superClass.getSuperclass();
        }

        return Optional.empty();
    }
}
