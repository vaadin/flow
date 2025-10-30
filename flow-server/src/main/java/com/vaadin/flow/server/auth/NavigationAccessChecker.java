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

import java.io.Serializable;

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
 * {@link AccessCheckResult}s.
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
     * The checker may grant access, deny it, or abstain from taking a decision,
     * by returning an appropriate {@link AccessCheckResult} object.
     *
     * <pre>
     *{@code
     * public AccessCheckResult check(NavigationContext context) {
     *     if (canHandleNavigationRequest(context)) {
     *         if (hasAccess(context)) {
     *             return AccessCheckResult.allow();
     *         } else {
     *             return AccessCheckResult.deny("Access denied");
     *         }
     *     }
     *     return AccessCheckResult.neutral();
     * }
     * }
     * </pre>
     *
     * A special case of deny is {@literal rejection}; a
     * {@link AccessCheckDecision#REJECT} result should be returned if there are
     * misconfiguration in security setup or critical unexpected runtime that
     * prevent the {@link NavigationAccessChecker} from taking the access
     * decision.
     *
     * <pre>
     *{@code
     * public AccessCheckResult check(NavigationContext context) {
     *     try {
     *         if (hasAccess(context)) {
     *             return AccessCheckResult.allow();
     *         } else {
     *             return AccessCheckResult.deny("Access denied");
     *         }
     *     } catch (Exception ex) {
     *         return AccessCheckResult
     *                 .reject("Cannot determine if access can be granted: "
     *                         + ex.getMessage());
     *     }
     * }
     * }
     * </pre>
     *
     * Result object can also be created using {@link NavigationContext} helpers
     * {@link NavigationContext#allow()},
     * {@link NavigationContext#deny(String)},
     * {@link NavigationContext#reject(String)} and
     * {@link NavigationContext#neutral()}.
     *
     * <p>
     *
     * The check is performed for both regular navigation and during error
     * handling rerouting. The current phase can be checked with the
     * {@link NavigationContext#isErrorHandling()} flag. The checker
     * implementation can decide to ignore the error handling phase, by
     * returning a {@link NavigationContext#neutral()} result.
     * <p>
     *
     * Method implementation is not supposed to throw any kind of exception.
     *
     * @param context
     *            the current navigation context
     * @return a result indicating weather the access to target view should be
     *         granted or not, never {@literal null}.
     */
    AccessCheckResult check(NavigationContext context);

}
