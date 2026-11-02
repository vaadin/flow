/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;

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
 * 
 * @since 24.3
 */
public final class NavigationContext {

    /**
     * The reason why an access check is run.
     */
    public enum Phase {
        /**
         * The current user navigates to the target view.
         */
        NAVIGATION,
        /**
         * The access of the current user to the target view is checked outside
         * of a navigation, for example to populate the menu.
         */
        EVALUATION,
        /**
         * The target view is checked for anonymous access, to find out if the
         * route is public. No user is involved: the principal is {@code null}
         * and no role is granted.
         */
        PUBLIC_ROUTE_CHECK
    }

    private final Router router;
    private final Class<?> navigationTarget;
    private final Location location;
    private final RouteParameters parameters;
    private final Principal principal;
    private final Predicate<String> roleChecker;

    private final boolean errorHandling;
    private final Phase phase;

    // Resolved on first use when the context is not created from an event
    private List<Class<? extends RouterLayout>> layouts;

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
     * @param navigating
     *            {@literal true} if the navigation is ongoing, {@literal false}
     *            if not (e.g. during access checks outside of navigation)
     * @since 25.0
     * @deprecated use
     *             {@link #NavigationContext(Router, Class, Location, RouteParameters, Principal, Predicate, boolean, Phase)}
     *             and give the {@link Phase} of the check
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public NavigationContext(Router router, Class<?> navigationTarget,
            Location location, RouteParameters parameters, Principal principal,
            Predicate<String> roleChecker, boolean errorHandling,
            boolean navigating) {
        this(router, navigationTarget, location, parameters, principal,
                roleChecker, errorHandling,
                navigating ? Phase.NAVIGATION : Phase.EVALUATION);
    }

    /**
     * Creates a new navigation context instance for the given phase.
     * <p>
     * A {@link Phase#PUBLIC_ROUTE_CHECK} context has no user, so it can only be
     * created with
     * {@link #forPublicRouteCheck(Router, Class, Location, RouteParameters)}.
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
     * @param phase
     *            the reason why the access check is run, not {@literal null}
     * @throws IllegalArgumentException
     *             if the phase is {@link Phase#PUBLIC_ROUTE_CHECK}
     */
    public NavigationContext(Router router, Class<?> navigationTarget,
            Location location, RouteParameters parameters, Principal principal,
            Predicate<String> roleChecker, boolean errorHandling, Phase phase) {
        this(requireUserPhase(phase), router, navigationTarget, location,
                parameters, principal, roleChecker, errorHandling);
    }

    private NavigationContext(Phase phase, Router router,
            Class<?> navigationTarget, Location location,
            RouteParameters parameters, Principal principal,
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
        this.phase = phase;
    }

    private static Phase requireUserPhase(Phase phase) {
        Objects.requireNonNull(phase, "phase must not be null");
        if (phase == Phase.PUBLIC_ROUTE_CHECK) {
            throw new IllegalArgumentException(
                    "A PUBLIC_ROUTE_CHECK context has no user, use forPublicRouteCheck(...) to create it");
        }
        return phase;
    }

    /**
     * Creates a new navigation context instance for ongoing navigation.
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
        this(router, navigationTarget, location, parameters, principal,
                roleChecker, errorHandling, true);
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
        this.layouts = List.copyOf(event.getLayouts());
    }

    /**
     * Creates a navigation context to find out if the target view is public,
     * that is, if an anonymous user has access to it.
     * <p>
     * The context has no principal and no role is granted. The phase is
     * {@link Phase#PUBLIC_ROUTE_CHECK}.
     *
     * @param router
     *            the router, not {@literal null}
     * @param navigationTarget
     *            navigation target class, not {@literal null}
     * @param location
     *            the requested location, not {@literal null}
     * @param parameters
     *            route parameters, not {@literal null}
     * @return a navigation context for the public route check
     */
    public static NavigationContext forPublicRouteCheck(Router router,
            Class<?> navigationTarget, Location location,
            RouteParameters parameters) {
        return new NavigationContext(Phase.PUBLIC_ROUTE_CHECK, router,
                navigationTarget, location, parameters, null, role -> false,
                false);
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
     * Gets the reason why the access check is run.
     *
     * @return the phase of the access check, not {@code null}
     */
    public Phase getPhase() {
        return phase;
    }

    /**
     * Gets if navigation is ongoing or not, that is, if the phase is
     * {@link Phase#NAVIGATION}. If not, navigation context is not expected to
     * produce access denial warnings in the logs eagerly.
     *
     * @return {@literal true} if the navigation is ongoing, {@literal false} if
     *         not
     * @since 25.0
     */
    public boolean isNavigating() {
        return phase == Phase.NAVIGATION;
    }

    /**
     * Gets the prefix for log messages written during this check, so a public
     * route check can be told apart from a check of the current user.
     *
     * @return the prefix, or an empty string for a check of the current user
     */
    String getLogPrefix() {
        return phase == Phase.PUBLIC_ROUTE_CHECK ? "Public route check: " : "";
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
     * Gets the layouts the navigation target is shown in, starting from the
     * layout closest to the navigation target.
     * <p>
     * During navigation, these are the layouts the router creates for the
     * matched route or route alias, including an automatic layout. Outside of
     * navigation, they are resolved from the route registry for the location.
     *
     * @return an unmodifiable list of the layouts of the navigation target, or
     *         an empty list if there are none, not {@code null}
     */
    public List<Class<? extends RouterLayout>> getLayouts() {
        if (layouts == null) {
            layouts = List.copyOf(
                    RouteUtil.resolveTargetParentLayouts(router.getRegistry(),
                            navigationTarget, location.getPath()));
        }
        return layouts;
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
