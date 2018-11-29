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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.startup.GlobalRouteRegistry;
import com.vaadin.flow.server.startup.RouteTarget;

/**
 * AbstractRouteRegistry with locking support and configuration.
 */
public abstract class AbstractRouteRegistry implements RouteRegistry {

    /**
     * Configuration interface to use for updating the configuration entity
     */
    @FunctionalInterface
    public interface Configuration extends Serializable {
        /**
         * Configure the given RouteConfiguration.
         *
         * @param configuration
         *         mutable routeConfiguration to make changes to
         */
        void configure(RouteConfiguration configuration);
    }

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    // The live configuration for this route registry
    protected volatile RouteConfiguration routeConfiguration = new RouteConfiguration();

    /**
     * Thread-safe update of the RouteConfiguration.
     *
     * @param command
     *         command that will mutate the configuration copy.
     */
    protected void configure(Configuration command) {
        configurationLock.lock();
        try {
            RouteConfiguration mutableCopy = getRouteConfiguration(
                    routeConfiguration, true);

            command.configure(mutableCopy);

            routeConfiguration = getRouteConfiguration(mutableCopy, false);
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Get a copy of the RouteConfiguration as mutable ot immutable.
     *
     * @param original
     *         the latest route configuration
     * @param mutable
     *         set the configuration as mutable or immutable
     * @return new router configuration object
     */
    protected RouteConfiguration getRouteConfiguration(
            RouteConfiguration original, boolean mutable) {
        return new RouteConfiguration(original, mutable);
    }

    @Override
    public List<RouteData> getRegisteredRoutes() {
        List<RouteData> registeredRoutes = new ArrayList<>();
        routeConfiguration.getTargetRoutes().forEach((target, url) -> {
            List<Class<?>> parameters = getRouteParameters(target);

            RouteData route = new RouteData(getParentLayout(target), url,
                    parameters, target);
            registeredRoutes.add(route);
        });

        Collections.sort(registeredRoutes);

        return Collections.unmodifiableList(registeredRoutes);
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(
            Class<? extends Component> navigationTarget, String path) {
        return RouteUtil.getParentLayouts(navigationTarget, path);
    }

    @Override
    public List<Class<? extends RouterLayout>> getNonRouteLayouts(
            Class<? extends Component> errorTarget) {
        return RouteUtil.getParentLayoutsForNonRouteTarget(errorTarget);
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
     *         navigation target to generate url for
     * @return route with required parameters
     */
    private String collectRequiredParameters(
            Class<? extends Component> navigationTarget) {
        StringBuilder route = new StringBuilder(
                routeConfiguration.getTargetRoute(navigationTarget));

        List<Class<?>> routeParameters = getRouteParameters(navigationTarget);

        if (!routeParameters.isEmpty()) {
            routeParameters.forEach(
                    param -> route.append("/{").append(param.getSimpleName())
                            .append("}"));
        }
        return route.toString();
    }

    /**
     * Add the given navigation target as a route to the configuration.
     * <p>
     * Note! This is a helper class and requires that the caller gives a mutable
     * {@link RouteConfiguration} and has the configuration lock.
     *
     * @param navigationTarget
     *         navigation target to register
     * @param configuration
     *         configuration to add route to
     * @throws InvalidRouteConfigurationException
     *         if a exact match route exists already
     */
    protected void setRoute(Class<? extends Component> navigationTarget,
            RouteConfiguration configuration)
            throws InvalidRouteConfigurationException {
        Logger logger = LoggerFactory
                .getLogger(GlobalRouteRegistry.class.getName());

        Set<String> routeAndRouteAliasPaths = new HashSet<>();

        String route = RouteUtil.getNavigationRouteAndAliases(navigationTarget,
                routeAndRouteAliasPaths);
        routeAndRouteAliasPaths.add(route);

        for (String path : routeAndRouteAliasPaths) {
            if (configuration.hasRoute(path)) {
                configuration.addRouteTarget(path, navigationTarget);
            } else {
                logger.debug(
                        "Registering route '{}' to navigation target '{}'.",
                        path, navigationTarget.getName());

                RouteTarget routeTarget = new RouteTarget(navigationTarget);
                configuration.setRouteTarget(path, routeTarget);
            }
        }
        configuration.putTargetRoute(navigationTarget, route);
    }
}
