/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;

/**
 * Checks if a user is allowed to navigate to a specific view.
 * <p>
 * Implementors of this interface are responsible to analyze a navigation
 * request and decide if the associate user (or anonymous) is granted to access
 * the target view.
 * <p>
 * The {@link NavigationContext} object provide information about the ongoing
 * navigation and the current user and its assigned roles.
 * <p>
 * Based on the context information the navigation access checker must take a
 * decision about the current navigation, that can be one of:
 *
 * <ul>
 * <li>Allow: the navigation is permitted</li>
 * <li>Deny: the navigation is denied</li>
 * <li>Reject: the navigation is denied because of a configuration or
 * development mistake</li>
 * <li>Neutral: no opinions about the current navigation</li>
 * </ul>
 *
 * {@link NavigationContext} provides the methods to create the above
 * {@link Result}s.
 */
@FunctionalInterface
public interface NavigationAccessChecker extends Serializable {

    /**
     * Checks if the current user is allowed to access a target view.
     * <p>
     * Details about the navigation target and user are provided by the
     * {@link NavigationContext} object.
     * <p>
     * The path is relative to the Vaadin application and does not contain
     * container specific details such as context path or servlet path.
     * <p>
     * The checker may grant access ({@link NavigationContext#allow()}, deny it
     * ({@link NavigationContext#deny(String)},
     * {@link NavigationContext#reject(String)}), or abstain from taking a
     * decision ({@link NavigationContext#neutral()}.
     * <p>
     * The check is performed for both regular navigation and for error handling
     * rerouting. The current phase can be checked with the
     * {@link NavigationContext#isErrorHandling()} flag. The checker
     * implementation can decide to ignore the error handling phase, by
     * returning a {@link NavigationContext#neutral()} result.
     * <p>
     * Method implementation is not supposed to throw any kind of exception.
     *
     * @param context
     *            the current navigation context
     * @return a result indicating weather the access to target view should be
     *         granted or not, never {@literal null}.
     */
    Result check(NavigationContext context);

    /**
     * Decision on navigation access.
     */
    enum Decision {
        /**
         * Allows access to the target view.
         */
        ALLOW,
        /**
         * Denies access to the target view.
         */
        DENY,
        /**
         * Denies access to the target view because of a critical permission
         * configuration mistake.
         */
        REJECT,
        /**
         * Abstains from taking a decision about access to the target view.
         */
        NEUTRAL
    }

    /**
     * Indicates a class that is responsible for taking a decisions about
     * granting access to a target view, based on the result provided by
     * {@link NavigationAccessChecker}s.
     * <p>
     * The component is used by {@link NavigationAccessControl} to compute the
     * final decision, based on the results of all registered
     * {@link NavigationAccessChecker}s.
     */
    @FunctionalInterface
    interface DecisionResolver extends Serializable {

        /**
         * Determines if access is granted for a specific navigation context,
         * based on the decisions provided by {@link NavigationAccessChecker}s.
         * <p>
         * The {@link NavigationContext} object provides necessary methods to
         * create the result.
         * <p>
         * A {@link Decision#NEUTRAL} result produces the same effect as
         * {@link Decision#REJECT}.
         *
         * <pre>{@code
         * Result resolve(List<Result> results, NavigationContext context) {
         *
         *     Map<Decision, Long> votes = results.stream()
         *             .collect(groupingBy(Result::decision, counting()));
         *
         *     int allow = votes.getOrDefault(Decision.ALLOW, 0L).intValue();
         *     int deny = votes.getOrDefault(Decision.ALLOW, 0L).intValue();
         *     int diff = allow - deny;
         *
         *     if (allow == 0 && deny == 0) {
         *         return context.neutral();
         *     } else if (diff > 0) {
         *         return context.allow();
         *     } else if (diff < 0) {
         *         return context.deny(String.format(
         *                 "Allow %d - Deny %d. Deny wins!", allow, deny));
         *     }
         *     return context.reject(
         *             "To allow, or not to allow, that is the question.");
         * }
         * }</pre>
         *
         * @param results
         *            the decisions from access checkers.
         * @param context
         *            the current navigation context
         * @return a result indicating weather the access to target view should
         *         be granted or not, never {@literal null}.
         */
        Result resolve(List<Result> results, NavigationContext context);
    }

    /**
     * Context information for a navigation request.
     * <p>
     * The navigation context is responsible for providing details about the
     * current navigation, such as the navigation target, the location and the
     * current user, and to allow {@link NavigationAccessChecker} to create a
     * representation of their decisions.
     * <p>
     * A {@link NavigationAccessChecker} should use {@link #allow()},
     * {@link #neutral()}, {@link #deny(String)} and {@link #reject(String)}
     * methods to create a result in
     * {@link NavigationAccessChecker#check(NavigationContext)} implementation.
     *
     * <ul>
     * <li>{@link #allow()} created a result whose meaning is that the access to
     * a view is granted.</li>
     * <li>{@link #neutral()} means that the access checker is not able to
     * determine if the current navigation should be allowed or not.</li>
     * <li>{@link #deny(String)} is used to create a response that will prevent
     * the navigation to the target view. The given reason should provide
     * details that will help to debug access control issues.</li>
     * <li>{@link #reject(String)} denies the access to the target view, but
     * should be used to indicate mistakes in security configuration that do not
     * allow the navigation checker to take a decision; for example, a
     * configuration where the path {@literal /my/view} is public, but
     * {@literal /my/*} is protected.</li>
     * </ul>
     */
    final class NavigationContext {
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
                Location location, RouteParameters parameters,
                Principal principal, Predicate<String> roleChecker,
                boolean errorHandling) {
            this.router = Objects.requireNonNull(router,
                    "router must no be null");
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
         * {@link com.vaadin.flow.router.HasErrorParameter} component
         * responsible to manage cope with the raised exception.
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
         * @return a representation of the currently logged in user or
         *         {@code null} if no user is currently logged in
         *
         */
        public Principal getPrincipal() {
            return principal;
        }

        /**
         * Gets if current navigation change is related to error handling or if
         * it is a regular navigation to a target view.
         *
         * @return {@literal true} if the current navigation is related to an
         *         error handling phase, {@literal false} for a regular
         *         navigation to a target view
         */
        public boolean isErrorHandling() {
            return errorHandling;
        }

        /**
         * Gets if the current user belongs the specified logical role.
         *
         * @param role
         *            a String specifying the name of the role
         * @return {@literal true} if the current user belongs to the given
         *         role, {@literal false} otherwise
         *
         */
        public boolean hasRole(String role) {
            return roleChecker.test(role);
        }

        /**
         * Create a result instance informing that the navigation to the target
         * view is allowed for the current user.
         *
         * @return a {@link Decision#ALLOW} result instance.
         */
        public Result allow() {
            return Result.ALLOW;
        }

        /**
         * Create a result instance informing that the checker cannot take a
         * decision based on the given navigation information.
         *
         * @return a {@link Decision#NEUTRAL} result instance.
         */
        public Result neutral() {
            return Result.NEUTRAL;
        }

        /**
         * Create a result instance informing that the navigation to the target
         * view is denied for the current user.
         *
         * @param reason
         *            a message explaining why the navigation has been denied.
         *            Useful for debugging purposes.
         *
         * @return a {@link Decision#DENY} result instance.
         */
        public Result deny(String reason) {
            return new Result(Decision.DENY, reason);
        }

        /**
         * Create a result instance informing that the navigation to the target
         * view is denied for the current user because of a misconfiguration or
         * a critical development time error.
         *
         * @param reason
         *            a message explaining why the navigation has been denied
         *            and the critical issue encountered. Useful for debugging
         *            purposes.
         *
         * @return a {@link Decision#REJECT result instance.
         */
        public Result reject(String reason) {
            return new Result(Decision.REJECT, reason);
        }

    }

    /**
     * A representation of the access check result, potentially providing deny
     * reason.
     */
    final class Result implements Serializable {

        /**
         * Default allow instance
         */
        static final Result ALLOW = new Result(Decision.ALLOW, null);
        /**
         * Default neutral instance
         */
        static final Result NEUTRAL = new Result(Decision.NEUTRAL, null);
        private final String reason;

        private final Decision decision;

        /**
         * Creates a new result.
         *
         * @param decision
         *            the access checker decision.
         * @param reason
         *            a message explaining the reason for that decision.
         */
        Result(Decision decision, String reason) {
            this.decision = decision;
            this.reason = reason;
        }

        /**
         * Gets the navigation access checker decision.
         *
         * @return the navigation access checker decision, never
         *         {@literal null}.
         */
        public Decision decision() {
            return decision;
        }

        /**
         * Gets the reason for the navigation access checker decision.
         * <p>
         * May be null for {@literal allow and neutral} decisions.
         *
         * @return the reason for the navigation access checker decision.
         */
        public String reason() {
            return reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Result result = (Result) o;
            return Objects.equals(reason, result.reason)
                    && decision == result.decision;
        }

        @Override
        public int hashCode() {
            return Objects.hash(reason, decision);
        }

        @Override
        public String toString() {
            return "Access decision: " + decision
                    + (reason != null ? (". " + reason) : "");
        }
    }
}
