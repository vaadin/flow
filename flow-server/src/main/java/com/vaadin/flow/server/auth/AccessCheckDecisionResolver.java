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
import java.util.List;

/**
 * Indicates a class that is responsible for taking a decisions about granting
 * access to a target view, based on the result provided by
 * {@link NavigationAccessChecker}s.
 * <p>
 * The component is used by {@link NavigationAccessControl} to compute the final
 * decision, based on the results of all registered
 * {@link NavigationAccessChecker}s.
 */
@FunctionalInterface
public interface AccessCheckDecisionResolver extends Serializable {

    /**
     * Determines if access is granted for a specific navigation context, based
     * on the decisions provided by {@link NavigationAccessChecker}s.
     * <p>
     *
     * The decision resolver should grant access or deny it by returning an
     * appropriate {@link AccessCheckResult} object.
     * <p>
     *
     * The expected result of the method should be
     * {@link AccessCheckDecision#ALLOW} or {@link AccessCheckDecision#DENY}, or
     * {@link AccessCheckDecision#REJECT}.
     * <p>
     *
     * A {@link AccessCheckDecision#NEUTRAL} result does not make because it
     * does not provide meaningful information to
     * {@link NavigationAccessControl} to complete the access check process. For
     * this reason, a neutral result will produce the same effect as
     * {@link AccessCheckDecision#REJECT}, preventing the navigation and failing
     * with an exception in development mode.
     *
     * <pre>{@code
     * AccessCheckResult resolve(List<Result> results,
     *         NavigationContext context) {
     *
     *     Map<Decision, Long> votes = results.stream()
     *             .collect(groupingBy(Result::decision, counting()));
     *
     *     int allow = votes.getOrDefault(Decision.ALLOW, 0L).intValue();
     *     int deny = votes.getOrDefault(Decision.ALLOW, 0L).intValue();
     *     int diff = allow - deny;
     *
     *     if (allow == 0 && deny == 0) {
     *         return AccessCheckResult.neutral();
     *     } else if (diff > 0) {
     *         return AccessCheckResult.allow();
     *     } else if (diff < 0) {
     *         return AccessCheckResult.deny(String
     *                 .format("Allow %d - Deny %d. Deny wins!", allow, deny));
     *     }
     *     return AccessCheckResult
     *             .reject("To allow, or not to allow, that is the question.");
     * }
     * }</pre>
     *
     * Result object can also be created using {@link NavigationContext} helpers
     * {@link NavigationContext#allow()},
     * {@link NavigationContext#deny(String)},
     * {@link NavigationContext#reject(String)} and
     * {@link NavigationContext#neutral()}.
     *
     * @param results
     *            the decisions from access checkers.
     * @param context
     *            the current navigation context
     * @return a result indicating weather the access to target view should be
     *         granted or not, never {@literal null}.
     */
    AccessCheckResult resolve(List<AccessCheckResult> results,
            NavigationContext context);
}
