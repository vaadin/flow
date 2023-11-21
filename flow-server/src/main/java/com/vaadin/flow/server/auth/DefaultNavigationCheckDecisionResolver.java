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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.auth.NavigationAccessChecker.AccessCheckResult;
import com.vaadin.flow.server.auth.NavigationAccessChecker.NavigationContext;

/**
 * Default implementation of {@link NavigationAccessChecker.DecisionResolver}
 * that allow access only if input results are all ALLOW, or a combination of
 * ALLOW and NEUTRAL. In any other case the access is DENIED.
 * <p>
 *
 * <pre>
 * | Results         | Decision |
 * | --------------- | -------- |
 * | All ALLOW       | ALLOW    |
 * | ALLOW + NEUTRAL | ALLOW    |
 * | All DENY        | DENY     |
 * | DENY + NEUTRAL  | DENY     |
 * | ALL NEUTRAL     | DENY     |
 * | ALLOW + DENY    | REJECT   |
 * </pre>
 *
 * <p>
 * </p>
 * Almost the same rule applies also if the evaluation happens during error
 * handling phase ({@link NavigationContext#isErrorHandling()} is
 * {@literal true}), with a single exception: in this case, if all the results
 * are {@literal NEUTRAL} the access is granted because the target of the
 * navigation is supposed to be an error handler component and not a view with
 * sensible information.
 * <p>
 * </p>
 * To be noted that the above situation never happens if the
 * {@link AnnotatedViewAccessChecker} is enabled because it computes only ALLOW
 * or DENY results.
 *
 */
public class DefaultNavigationCheckDecisionResolver
        implements NavigationAccessChecker.DecisionResolver {

    public static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultNavigationCheckDecisionResolver.class);

    @Override
    public AccessCheckResult resolve(List<AccessCheckResult> results,
            NavigationContext context) {
        Class<?> navigationTarget = context.getNavigationTarget();
        String path = context.getLocation().getPath();
        Map<NavigationAccessChecker.Decision, List<AccessCheckResult>> resultsByDecision = results
                .stream()
                .collect(Collectors.groupingBy(AccessCheckResult::decision));
        int neutralVotes = Optional
                .ofNullable(resultsByDecision
                        .remove(NavigationAccessChecker.Decision.NEUTRAL))
                .map(Collection::size).orElse(0);

        String denyReasons = resultsByDecision
                .getOrDefault(NavigationAccessChecker.Decision.DENY, List.of())
                .stream().map(AccessCheckResult::reason)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(System.lineSeparator()));

        if (resultsByDecision.size() == 1) {
            // Unanimous consensus
            NavigationAccessChecker.Decision decision = resultsByDecision
                    .keySet().iterator().next();
            int votes = resultsByDecision.get(decision).size();
            if (decision == NavigationAccessChecker.Decision.ALLOW) {
                LOGGER.debug("Access to view '{}' with path '{}' allowed by "
                        + "{} out of {} navigation checkers  ({} neutral).",
                        navigationTarget.getName(), path, votes, results.size(),
                        neutralVotes);
                return context.allow();
            } else {
                LOGGER.debug("Access to view '{}' with path '{}' denied by "
                        + "{} out of {} navigation checkers  ({} neutral).",
                        navigationTarget.getName(), path, votes, results.size(),
                        neutralVotes);
            }
        } else if (resultsByDecision.isEmpty()) {
            // All checkers are neutral
            if (context.isErrorHandling()) {
                // During a re-route to an error handler component navigation
                // access checkers may abstain from taking a decision because
                // HasErrorParameter classes are usually not routes with a path,
                // but simple Flow components.
                // For those error handling view access is allowed, if none of
                // the checkers explicitly denies it.
                return context.allow();
            }
            denyReasons = "Access denied because navigation checkers did not take any decision.";
            LOGGER.debug(
                    "Access to view '{}' with path '{}' denied because "
                            + "{} out of {} navigation checkers are neutral.",
                    navigationTarget.getName(), path, results.size(),
                    results.size());
        } else {
            // Mixed consensus
            String summary = resultsByDecision.entrySet().stream()
                    .map(e -> e.getKey() + " = " + e.getValue().size())
                    .collect(Collectors.joining(", ", "Votes: ", ""));
            if (neutralVotes > 0) {
                summary += ", " + NavigationAccessChecker.Decision.NEUTRAL
                        + " = " + neutralVotes;
            }
            LOGGER.warn("Access to view '{}' with path '{}' blocked because "
                    + "there is no unanimous consensus from the navigation checkers. {}.",
                    navigationTarget.getName(), path, summary);
            return context.reject(String.format(
                    "Mixed consensus from navigation checkers for view '%s'"
                            + " with path '%s'. %s. Deny reasons: [%s]",
                    navigationTarget.getName(), path, summary, denyReasons));

        }
        return context.deny(denyReasons);
    }

}
