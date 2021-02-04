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
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.RouteAliasData;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.router.RouteParameters;
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

    private static final String TARGET_MUST_NOT_BE_NULL = "Target must not be null.";

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
        return Registration.addAndRemove(routesChangedListeners, listener);
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
        configuration.getTargetRoutes().forEach(
                (target, template) -> populateRegisteredRoutes(configuration,
                        registeredRoutes, target, template));

        Collections.sort(registeredRoutes);

        return Collections.unmodifiableList(registeredRoutes);
    }

    private void populateRegisteredRoutes(ConfiguredRoutes configuration,
            List<RouteData> registeredRoutes, Class<? extends Component> target,
            String template) {
        List<RouteAliasData> routeAliases = new ArrayList<>();

        configuration.getRoutePaths(target).stream().filter(
                routePathTemplate -> !routePathTemplate.equals(template))
                .forEach(
                        aliasRoutePathTemplate -> routeAliases
                                .add(new RouteAliasData(
                                        getParentLayouts(configuration,
                                                aliasRoutePathTemplate),
                                        aliasRoutePathTemplate,
                                        configuration.getParameters(
                                                aliasRoutePathTemplate),
                                        target)));
        List<Class<? extends RouterLayout>> parentLayouts = getParentLayouts(
                configuration, template);
        RouteData route = new RouteData(parentLayouts, template,
                configuration.getParameters(template), target, routeAliases);
        registeredRoutes.add(route);
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
                    route.getParentLayouts(), route.getTemplate(),
                    route.getRouteParameters(), route.getNavigationTarget(),
                    Collections.emptyList());

            flatRoutes.add(nonAliasCollection);
            route.getRouteAliases().forEach(flatRoutes::add);
        }

        return flatRoutes;
    }

    private List<Class<? extends RouterLayout>> getParentLayouts(
            ConfiguredRoutes configuration, String template) {
        RouteTarget routeTarget = configuration.getRouteTarget(template);
        if (routeTarget != null) {
            return routeTarget.getParentLayouts();
        }
        return Collections.emptyList();
    }

    /**
     * @deprecated instead use {@link #getNavigationRouteTarget(String)} and
     *             retrieve the list of route layouts from the
     *             {@link RouteTarget} contained in the
     *             {@link NavigationRouteTarget}.
     * @see RouteTarget#getParentLayouts()
     */
    @Override
    @Deprecated
    public List<Class<? extends RouterLayout>> getRouteLayouts(String path,
            Class<? extends Component> navigationTarget) {
        return getConfiguration().getParentLayouts(path, navigationTarget);
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget, TARGET_MUST_NOT_BE_NULL);

        HasUrlParameterFormat.checkMandatoryParameter(navigationTarget, null);

        return Optional.ofNullable(
                getConfiguration().getTargetUrl(navigationTarget));
    }

    @Override
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget,
            RouteParameters parameters) {
        Objects.requireNonNull(navigationTarget, TARGET_MUST_NOT_BE_NULL);

        HasUrlParameterFormat.checkMandatoryParameter(navigationTarget,
                parameters);

        return Optional.ofNullable(getConfiguration()
                .getTargetUrl(navigationTarget, parameters));
    }

    @Override
    public Optional<String> getTemplate(
            Class<? extends Component> navigationTarget) {
        Objects.requireNonNull(navigationTarget, TARGET_MUST_NOT_BE_NULL);

        return Optional.ofNullable(
                getConfiguration().getTemplate(navigationTarget));
    }

    @Override
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        configureWithFullTemplate(path, navigationTarget,
                (configuration, fullTemplate) -> configuration
                        .setRoute(fullTemplate, navigationTarget, parentChain));
    }

    @Override
    public void removeRoute(Class<? extends Component> navigationTarget) {
        if (!getConfiguration().hasRouteTarget(navigationTarget)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(navigationTarget));
    }

    @Override
    public void removeRoute(String path) {
        if (!getConfiguration().hasTemplate(path)) {
            return;
        }
        configure(configuration -> configuration.removeRoute(path));
    }

    @Override
    public void removeRoute(String path,
            Class<? extends Component> navigationTarget) {
        if (!getConfiguration().hasTemplate(path)) {
            return;
        }
        configureWithFullTemplate(path, navigationTarget,
                (configuration, fullTemplate) -> configuration
                        .removeRoute(fullTemplate, navigationTarget));
    }

    @Override
    public void clean() {
        configure(ConfigureRoutes::clear);
    }

    private void configureWithFullTemplate(String path,
            Class<? extends Component> navigationTarget,
            SerializableBiConsumer<ConfigureRoutes, String> templateConfiguration) {
        configure(configuration -> {
            templateConfiguration.accept(configuration,
                    HasUrlParameterFormat.getTemplate(path, navigationTarget));
        });
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
