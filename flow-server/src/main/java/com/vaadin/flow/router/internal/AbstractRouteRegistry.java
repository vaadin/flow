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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameterUtil;
import com.vaadin.flow.router.RouteAliasData;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameterFormat;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.UrlParameters;
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
        configuration.getTargetRoutes()
                .forEach((target, targetRoutePathTemplate) -> {

                    List<RouteAliasData> routeAliases = new ArrayList<>();

                    configuration.getRoutePaths(target).stream()
                            .filter(routePathTemplate -> !routePathTemplate
                                    .equals(targetRoutePathTemplate))
                            .forEach(aliasRoutePathTemplate -> routeAliases
                                    .add(new RouteAliasData(
                                            getParentLayouts(configuration,
                                                    target,
                                                    aliasRoutePathTemplate),
                                            aliasRoutePathTemplate,
                                            configuration.getParameters(
                                                    aliasRoutePathTemplate),
                                            target)));
                    List<Class<? extends RouterLayout>> parentLayouts = getParentLayouts(
                            configuration, target, targetRoutePathTemplate);
                    RouteData route = new RouteData(parentLayouts,
                            targetRoutePathTemplate,
                            configuration
                                    .getParameters(targetRoutePathTemplate),
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
                    route.getParentLayouts(), route.getUrlTemplate(),
                    route.getDefinedParameters(), route.getNavigationTarget(),
                    Collections.emptyList());

            flatRoutes.add(nonAliasCollection);
            route.getRouteAliases().forEach(flatRoutes::add);
        }

        return flatRoutes;
    }

    private List<Class<? extends RouterLayout>> getParentLayouts(
            ConfiguredRoutes configuration, Class<? extends Component> target,
            String urlTemplate) {
        RouteTarget routeTarget = configuration.getRouteTarget(urlTemplate);
        if (routeTarget != null) {
            return routeTarget.getParentLayouts();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(String url,
            Class<? extends Component> navigationTarget) {
        return getConfiguration().getParentLayouts(url, navigationTarget);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget, "Target must not be null.");

        HasUrlParameterUtil.checkMandatoryParameter(navigationTarget, null);

        return Optional.ofNullable(
                getConfiguration().getTargetUrl(navigationTarget));
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget,
            UrlParameters parameters) {
        Objects.requireNonNull(navigationTarget, "Target must not be null.");

        HasUrlParameterUtil.checkMandatoryParameter(navigationTarget,
                parameters);

        return Optional.ofNullable(getConfiguration()
                .getTargetUrl(navigationTarget, parameters));
    }

    @Override
    public Optional<String> getUrlTemplate(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget, "Target must not be null.");

        return Optional.ofNullable(
                getConfiguration().getUrlTemplate(navigationTarget));
    }

    @Override
    public Optional<String> getUrlTemplate(
            Class<? extends Component> navigationTarget,
            Set<RouteParameterFormat> format) {
        Objects.requireNonNull(navigationTarget, "Target must not be null.");

        return Optional.ofNullable(
                getConfiguration().getUrlTemplate(navigationTarget, format));
    }

    @Override
    public void setRoute(String urlTemplate,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        configure(configuration -> {
            addRouteToConfiguration(urlTemplate, navigationTarget, parentChain,
                    configuration);
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
    public void removeRoute(String urlTemplate) {
        if (!getConfiguration().hasRoute(urlTemplate)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(urlTemplate));
    }

    @Override
    public void removeRoute(String urlTemplate,
            Class<? extends Component> navigationTarget) {
        if (!getConfiguration().hasRoute(urlTemplate)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(urlTemplate,
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
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @param configuration
     *            mutable configuration object
     */
    private void addRouteToConfiguration(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain,
            ConfigureRoutes configuration) {
        if (!hasLock()) {
            throw new IllegalStateException(
                    "addRouteToConfiguration requires the registry lock and a mutable configuration.");
        }

        // Backward compatibility with HasUrlParameter for which the parameters
        // were stored in RouteTarget.
        path = HasUrlParameterUtil.getUrlTemplate(path, navigationTarget);

        configuration.setRoute(path, navigationTarget, parentChain);
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
