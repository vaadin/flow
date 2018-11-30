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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

/**
 * Route target holder that handles getting the correct type of has parameter
 * target.
 */
public class RouteTarget implements Serializable {
    private Class<? extends Component> normal;
    private Class<? extends Component> parameter;
    private Class<? extends Component> optionalParameter;
    private Class<? extends Component> wildCardParameter;

    private RouteTarget() {
    }

    /**
     * Create a new Route target holder.
     *
     * @param target
     *         navigation target
     * @throws InvalidRouteConfigurationException
     *         exception for miss configured routes where navigation targets
     *         can not be clearly selected
     */
    public RouteTarget(Class<? extends Component> target) {
        addRoute(target);
    }

    /**
     * Add a new route navigation target.
     * <p>
     * When adding a new target it will be validated that it is a valid path to
     * add with the already existing navigation targets.
     *
     * @param target
     *         navigation target to add
     * @throws InvalidRouteConfigurationException
     *         exception for miss configured routes where navigation targets
     *         can not be clearly selected
     */
    public void addRoute(Class<? extends Component> target) {
        if (!HasUrlParameter.class.isAssignableFrom(target)
                && !isAnnotatedParameter(target)) {
            validateNormalTarget(target);
            normal = target;
        } else {
            if (ParameterDeserializer
                    .isAnnotatedParameter(target, OptionalParameter.class)) {
                validateOptionalParameter(target);
                optionalParameter = target;
            } else if (ParameterDeserializer
                    .isAnnotatedParameter(target, WildcardParameter.class)) {
                validateWildcard(target);
                wildCardParameter = target;
            } else {
                validateParameter(target);
                parameter = target;
            }
        }
    }

    private void validateParameter(Class<? extends Component> target)
            throws InvalidRouteConfigurationException {
        if (parameter != null) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.",
                    parameter.getName(), target.getName()));
        }
    }

    private void validateWildcard(Class<? extends Component> target)
            throws InvalidRouteConfigurationException {
        if (wildCardParameter != null) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with wildcard parameter have the same route.",
                    wildCardParameter.getName(), target.getName()));
        }
    }

    private void validateOptionalParameter(Class<? extends Component> target)
            throws InvalidRouteConfigurationException {
        if (normal != null) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                    normal.getName(), target.getName(), target.getName()));
        } else if (optionalParameter != null) {
            String message = String
                    .format("Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.",
                            optionalParameter.getName(), target.getName());
            throw new InvalidRouteConfigurationException(message);
        }
    }

    private void validateNormalTarget(Class<? extends Component> target)
            throws InvalidRouteConfigurationException {
        if (normal != null) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.",
                    normal.getName(), target.getName()));
        } else if (optionalParameter != null) {
            throw new InvalidRouteConfigurationException(String.format(
                    "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                    target.getName(), optionalParameter.getName(),
                    optionalParameter.getName()));
        }
    }

    /**
     * Get route target for given segments.
     *
     * @param segments
     *         route segments
     * @return navigation target corresponding to given segments
     */
    public Class<? extends Component> getTarget(List<String> segments) {
        if (segments.isEmpty() && normal != null) {
            return normal;
        } else if (segments.size() == 1 && parameter != null) {
            return parameter;
        } else if (segments.size() <= 1 && optionalParameter != null) {
            return optionalParameter;
        } else if (wildCardParameter != null) {
            return wildCardParameter;
        }
        return null;
    }

    private boolean isAnnotatedParameter(Class<?> target) {
        return ParameterDeserializer
                .isAnnotatedParameter(target, OptionalParameter.class)
                || ParameterDeserializer
                .isAnnotatedParameter(target, WildcardParameter.class);
    }

    /**
     * Create a copy of this RouteTarget.
     *
     * @return copy of this RouteTarget
     */
    public RouteTarget copy() {
        RouteTarget copy = new RouteTarget();
        copy.normal = normal;
        copy.parameter = parameter;
        copy.optionalParameter = optionalParameter;
        copy.wildCardParameter = wildCardParameter;
        return copy;
    }

    /**
     * Remove target route from this RouteTarget.
     *
     * @param targetRoute
     *         route to remove
     */
    public void remove(Class<? extends Component> targetRoute) {
        if (targetRoute.equals(normal)) {
            normal = null;
        } else if (targetRoute.equals(parameter)) {
            parameter = null;
        } else if (targetRoute.equals(optionalParameter)) {
            optionalParameter = null;
        } else if (targetRoute.equals(wildCardParameter)) {
            wildCardParameter = null;
        }
    }

    /**
     * Check if navigation target is present in current target.
     *
     * @param target
     *         navigation target to check for
     * @return true if navigation target is found in some position
     */
    public boolean containsTarget(Class<? extends Component> target) {
        return getRoutes().contains(target);
    }

    /**
     * Check if this RouteTarget is empty. This means that it no longer contains
     * any route classes.
     *
     * @return true is no targets are found
     */
    public boolean isEmpty() {
        return normal == null && parameter == null && optionalParameter == null
                && wildCardParameter == null;
    }

    /**
     * Get all registered targets for this routeTarget as a iterable.
     *
     * @return all registered route classes
     */
    public List<Class<? extends Component>> getRoutes() {
        List<Class<? extends Component>> registrations = new ArrayList<>(4);
        if (normal != null) {
            registrations.add(normal);
        } else if (parameter != null) {
            registrations.add(parameter);
        } else if (optionalParameter != null) {
            registrations.add(optionalParameter);
        } else if (wildCardParameter != null) {
            registrations.add(wildCardParameter);
        }

        return registrations;
    }
}
