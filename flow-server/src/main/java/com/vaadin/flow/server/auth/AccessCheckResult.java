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
import java.util.Objects;

/**
 * A representation of the access check result, potentially providing deny
 * reason.
 */
public class AccessCheckResult implements Serializable {

    /**
     * A result instance informing that the navigation to the target view is
     * allowed for the current user.
     */
    private static final AccessCheckResult ALLOW = new AccessCheckResult(
            AccessCheckDecision.ALLOW, null);
    /**
     * A result instance informing that the checker cannot take a decision based
     * on the given navigation information.
     */
    private static final AccessCheckResult NEUTRAL = new AccessCheckResult(
            AccessCheckDecision.NEUTRAL, null);
    private final String reason;

    private final AccessCheckDecision decision;

    /**
     * Creates a new result.
     *
     * @param decision
     *            the access checker decision.
     * @param reason
     *            a message explaining the reason for that decision.
     */
    public AccessCheckResult(AccessCheckDecision decision, String reason) {
        if (decision == null) {
            throw new IllegalArgumentException("Decision must not be null");
        }
        if ((decision == AccessCheckDecision.DENY
                || decision == AccessCheckDecision.REJECT) && reason == null) {
            throw new IllegalArgumentException(
                    decision.name() + " requires a not null reason");
        }
        this.decision = decision;
        this.reason = reason;
    }

    /**
     * Gets the navigation access checker decision.
     *
     * @return the navigation access checker decision, never {@literal null}.
     */
    public AccessCheckDecision decision() {
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
        AccessCheckResult result = (AccessCheckResult) o;
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

    /**
     * Create a result instance for the provided decision and reason.
     * <p>
     * </p>
     * The {@code reason} cannot be {@literal null} for
     * {@link AccessCheckDecision#DENY} and {@link AccessCheckDecision#REJECT}.
     * For {@link AccessCheckDecision#ALLOW} the reason is ignored.
     *
     * @param decision
     *            the decision for this result, never {@literal null}.
     * @param reason
     *            a message explaining why the current decision has been taken.
     *            Useful for debugging purposes.
     * @return a result instance for given decision and reason.
     */
    public static AccessCheckResult create(AccessCheckDecision decision,
            String reason) {
        return new AccessCheckResult(decision, reason);
    }

    /**
     * Create a result instance informing that the navigation to the target view
     * is allowed for the current user.
     *
     * @return a {@link AccessCheckDecision#ALLOW} result instance.
     */
    public static AccessCheckResult allow() {
        return AccessCheckResult.ALLOW;
    }

    /**
     * Create a result instance informing that the checker cannot take a
     * decision based on the given navigation information.
     *
     * @return a {@link AccessCheckDecision#NEUTRAL} result instance.
     */
    public static AccessCheckResult neutral() {
        return AccessCheckResult.NEUTRAL;
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
    public static AccessCheckResult deny(String reason) {
        return new AccessCheckResult(AccessCheckDecision.DENY, reason);
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
    public static AccessCheckResult reject(String reason) {
        return new AccessCheckResult(AccessCheckDecision.REJECT, reason);
    }

}
