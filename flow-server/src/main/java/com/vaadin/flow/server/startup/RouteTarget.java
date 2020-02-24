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
 *
 */
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

/**
 * Route target stores the target component and parent layouts.
 *
 * @since 1.0
 */
public class RouteTarget implements Serializable {

    private Class<? extends Component> target;

    private List<Class<? extends RouterLayout>> parentLayouts;

    /**
     * Create a new Route target holder with the given target registered.
     * <p>
     * Note! This will create a mutable RouteTarget by default.
     *
     * @param target
     *            navigation target
     * @param parents
     *            parent layout chain
     * @throws InvalidRouteConfigurationException
     *             exception for miss configured routes where navigation targets
     *             can not be clearly selected
     */
    public RouteTarget(Class<? extends Component> target,
            List<Class<? extends RouterLayout>> parents) {
        this.target = target;
        this.parentLayouts = parents != null
                ? Collections.unmodifiableList(new ArrayList<>(parents))
                : Collections.emptyList();
    }

    /**
     * Create a new Route target holder with the given target registered.
     * <p>
     * Note! This will create a mutable RouteTarget by default.
     *
     * @param target
     *            navigation target
     * @throws InvalidRouteConfigurationException
     *             exception for miss configured routes where navigation targets
     *             can not be clearly selected
     * @deprecated use {@link #RouteTarget(Class, List)} instead.
     */
    @Deprecated
    public RouteTarget(Class<? extends Component> target) {
        setTarget(target);
    }

    /**
     * Create a new Route target holder with the given target registered.
     *
     * @param target
     *            navigation target
     * @param mutable
     *            if this should be mutable
     * @throws InvalidRouteConfigurationException
     *             exception for miss configured routes where navigation targets
     *             can not be clearly selected
     * @deprecated mutable argument is ignored.
     */
    @Deprecated
    public RouteTarget(Class<? extends Component> target, boolean mutable) {
        this(target);
    }

    /**
     * Sets the target if not null.
     *
     * @param target
     *            navigation target to add
     * @throws InvalidRouteConfigurationException
     *             exception for miss configured routes where navigation targets
     *             can not be clearly selected
     * @deprecated RouteTarget wraps only one target component and parameters
     *             are provided through {@link NavigationRouteTarget}
     */
    @Deprecated
    public void addRoute(Class<? extends Component> target) {
        throwIfImmutable();
        setTarget(target);
    }

    private void setTarget(Class<? extends Component> target) {
        if (this.target != null) {
            throw new AmbiguousRouteConfigurationException(String.format(
                    "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.",
                    this.target.getName(), target.getName()), this.target);
        }

        this.target = target;
    }

    /**
     * Get the component route target.
     *
     * @return component navigation target.
     */
    public Class<? extends Component> getTarget() {
        return target;
    }

    /**
     * Get route target for given segments.
     *
     * @param segments
     *            route segments
     * @return navigation target corresponding to given segments
     *
     * @deprecated use {@link #getTarget()} instead.
     */
    @Deprecated
    public Class<? extends Component> getTarget(List<String> segments) {
        return getTarget();
    }

    /**
     * Create a copy of this RouteTarget.
     *
     * @param mutable
     *            if created copy is mutable or not
     * @return copy of this RouteTarget
     */
    public RouteTarget copy(boolean mutable) {
        RouteTarget copy = new RouteTarget(target);
        copy.parentLayouts = parentLayouts;
        return copy;
    }

    /**
     * Remove target route from this RouteTarget. This will also clear the
     * parent layout chain for the target.
     *
     * @param targetRoute
     *            target which was set.
     * @deprecated
     */
    @Deprecated
    public void remove(Class<? extends Component> targetRoute) {
        throwIfImmutable();
        if (targetRoute.equals(target)) {
            target = null;
        }

        parentLayouts = null;
    }

    /**
     * Check if navigation target is present in current target.
     *
     * @param target
     *            navigation target to check for
     * @return true if navigation target is found in some position
     * @deprecated use {@link #getTarget()} instead
     */
    @Deprecated
    public boolean containsTarget(Class<? extends Component> target) {
        return Objects.equals(this.target, target);
    }

    /**
     * Check if this RouteTarget is empty. This means that it no longer contains
     * any route classes.
     *
     * @return true is no targets are found
     * @deprecated use {@link #getTarget()} instead
     */
    @Deprecated
    public boolean isEmpty() {
        return target == null;
    }

    /**
     * Get all registered targets for this routeTarget as a iterable.
     *
     * @return all registered route classes
     * @deprecated use {@link #getTarget()} instead
     */
    @Deprecated
    public List<Class<? extends Component>> getRoutes() {
        List<Class<? extends Component>> registrations = new ArrayList<>(4);
        if (target != null) {
            registrations.add(target);
        }
        return registrations;
    }

    /**
     * Set the parent layout chain for target component. This will override any
     * existing parent layout chain for the target.
     * <p>
     * Note! if adding parents for a non registered target an
     * IllegalArgumentException will be thrown.
     *
     * @param target
     *            target to add chain for
     * @param parents
     *            parent layout chain
     * @deprecated use {@link #setParentLayouts(List)} instead
     */
    @Deprecated
    public void setParentLayouts(Class<? extends Component> target,
            List<Class<? extends RouterLayout>> parents) {
        if (!containsTarget(target)) {
            throw new IllegalArgumentException(
                    "Tried to add parent layouts for a non existing target "
                            + target.getName());
        }
        setParentLayouts(parents);
    }

    /**
     * Set the parent layout chain for target component. This will override the
     * existing parent layout chain for the target.
     * 
     * @param parents
     *            parent layout chain
     */
    public void setParentLayouts(List<Class<? extends RouterLayout>> parents) {
        throwIfImmutable();
        parentLayouts = Collections.unmodifiableList(new ArrayList<>(parents));
    }

    /**
     * Get the parent layout chain defined for the given target.
     *
     * @param target
     *            target to get parent layout chain for
     * @return parent layout chain
     * @deprecated use {@link #getParentLayouts()} instead
     */
    @Deprecated
    public List<Class<? extends RouterLayout>> getParentLayouts(
            Class<? extends Component> target) {
        return getParentLayouts();
    }

    /**
     * Get the parent layout chain.
     * 
     * @return parent layout chain
     */
    public List<Class<? extends RouterLayout>> getParentLayouts() {
        if (parentLayouts == null) {
            return Collections.emptyList();
        }
        return parentLayouts;
    }

    private void throwIfImmutable() {
        throw new IllegalStateException(
                "Tried to mutate immutable configuration.");
    }
}
