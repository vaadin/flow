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
package com.vaadin.flow.server.auth;

import java.security.Principal;
import java.util.Objects;
import java.util.function.Predicate;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;

/**
 * Context information for a navigation request.
 * <p>
 * The navigation context is responsible for providing details about the current
 * navigation, such as the navigation target, the location and the current user,
 * and to allow {@link NavigationAccessChecker} to create a representation of
 * their decisions.
 * <p>
 * A {@link NavigationAccessChecker} should use {@link #allow()},
 * {@link #neutral()}, {@link #deny(String)} and {@link #reject(String)} methods
 * to create a result in
 * {@link NavigationAccessChecker#check(NavigationContext)} implementation.
 *
 * <ul>
 * <li>{@link #allow()} created a result whose meaning is that the access to a
 * view is granted.</li>
 * <li>{@link #neutral()} means that the access checker is not able to determine
 * if the current navigation should be allowed or not.</li>
 * <li>{@link #deny(String)} is used to create a response that will prevent the
 * navigation to the target view. The given reason should provide details that
 * will help to debug access control issues.</li>
 * <li>{@link #reject(String)} denies the access to the target view, but should
 * be used to indicate mistakes in security configuration that do not allow the
 * navigation checker to take a decision; for example, a configuration where the
 * path {@literal /my/view} is public, but {@literal /my/*} is protected.</li>
 * </ul>
 */
public final class NavigationContext {
    private final Router router;
    private final Class<?> navigationTarget;
    private final Location location;
    private final RouteParameters parameters;
    private final Principal principal;
    private final Predicate<String> roleChecker;

    private final boolean errorHandling;

    /**
     * Creates a new navigation context instance.
     *
     * @param router
     *            the router that triggered the change, not {@literal null}
     * @param navigationTarget
     *            navigation target class, not {@literal null}
     * @param location
     *            the requested location, not {@literal null}
     * @param parameters
     *            route parameters, not {@literal null}
     * @param principal
     *            the principal of the user
     * @param roleChecker
     *            a function that can answer if a user has a given role
     * @param errorHandling
     *            {@literal true} if the current navigation is related to an
     *            error handling phase, {@literal false} for a regular
     *            navigation to a target view
     */
    public NavigationContext(Router router, Class<?> navigationTarget,
            Location location, RouteParameters parameters, Principal principal,
            Predicate<String> roleChecker, boolean errorHandling) {
        this.router = Objects.requireNonNull(router, "router must no be null");
        this.navigationTarget = Objects.requireNonNull(navigationTarget,
                "navigationTarget must no be null");
        this.location = Objects.requireNonNull(location,
                "location must no be null");
        this.parameters = Objects.requireNonNull(parameters,
                "parameters must no be null");
        this.roleChecker = Objects.requireNonNull(roleChecker,
                "roleChecker must no be null");
        this.principal = principal;
        this.errorHandling = errorHandling;
    }

    /**
     * Create a new navigation context instance based on a
     * {@link BeforeEnterEvent}.
     *
     * @param event
     *            the event created before ongoing navigation happens.
     * @param principal
     *            the principal of the user
     * @param roleChecker
     *            a function that can answer if a user has a given role
     */
    public NavigationContext(BeforeEnterEvent event, Principal principal,
            Predicate<String> roleChecker) {
        this(event.getSource(), event.getNavigationTarget(),
                event.getLocation(), event.getRouteParameters(), principal,
                roleChecker, event.isErrorEvent());
    }

    /**
     * Gets the router that triggered the navigation change.
     *
     * @return router that triggered the navigation change, not {@code null}
     */
    public Router getRouter() {
        return router;
    }

    /**
     * Gets the navigation target.
     * <p>
     * In case of error handling, the navigation target refers to the
     * {@link com.vaadin.flow.router.HasErrorParameter} component responsible to
     * manage cope with the raised exception.
     *
     * @return navigation target, not {@code null}
     * @see #isErrorHandling()
     */
    public Class<?> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Gets the requested location.
     * <p>
     * Note that in case of error handling the location still references the
     * initial request.
     *
     * @return the requested location, not {@code null}
     * @see #isErrorHandling()
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the route parameters associated with this event.
     *
     * @return route parameters retrieved from the navigation url, not
     *         {@code null}
     */
    public RouteParameters getParameters() {
        return parameters;
    }

    /**
     * Gets the principal for the currently logged in user.
     *
     * @return a representation of the currently logged in user or {@code null}
     *         if no user is currently logged in
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Gets if current navigation change is related to error handling or if it
     * is a regular navigation to a target view.
     *
     * @return {@literal true} if the current navigation is related to an error
     *         handling phase, {@literal false} for a regular navigation to a
     *         target view
     */
    public boolean isErrorHandling() {
        return errorHandling;
    }

    /**
     * Gets if the current user belongs the specified logical role.
     *
     * @param role
     *            a String specifying the name of the role
     * @return {@literal true} if the current user belongs to the given role,
     *         {@literal false} otherwise
     */
    public boolean hasRole(String role) {
        return roleChecker.test(role);
    }

    /**
     * Create a result instance informing that the navigation to the target view
     * is allowed for the current user.
     *
     * @return a {@link AccessCheckDecision#ALLOW} result instance.
     */
    public AccessCheckResult allow() {
        return AccessCheckResult.allow();
    }

    /**
     * Create a result instance informing that the checker cannot take a
     * decision based on the given navigation information.
     *
     * @return a {@link AccessCheckDecision#NEUTRAL} result instance.
     */
    public AccessCheckResult neutral() {
        return AccessCheckResult.neutral();
    }

    /**
     * Create a result instance informing that the navigation to the target view
     * is denied for the current user.
     *
     * @param reason
     *            a message explaining why the navigation has been denied.
     *            Useful for debugging purposes.
     * @return a {@link AccessCheckDecision#DENY} result instance.
     */
    public AccessCheckResult deny(String reason) {
        return AccessCheckResult.deny(reason);
    }

    /**
     * Create a result instance informing that the navigation to the target view
     * is denied for the current user because of a misconfiguration or a
     * critical development time error.
     *
     * @param reason
     *            a message explaining why the navigation has been denied and
     *            the critical issue encountered. Useful for debugging purposes.
     * @return a {@link AccessCheckDecision#REJECT} result instance.
     */
    public AccessCheckResult reject(String reason) {
        return AccessCheckResult.reject(reason);
    }

}
