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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.NavigationContext.Phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultAccessCheckDecisionResolverTest {

    private final DefaultAccessCheckDecisionResolver resolver = new DefaultAccessCheckDecisionResolver();

    @Test
    void resolve_mixedVotes_navigation_rejectsAndLogsWarn() {
        NavigationContext context = new NavigationContext(mock(Router.class),
                AnonymousAllowedView.class, new Location("anon"),
                RouteParameters.empty(), null, role -> false, false,
                Phase.NAVIGATION);

        String logOutput = LogOutputCapture
                .capture(() -> assertEquals(AccessCheckDecision.REJECT,
                        resolveMixedVotes(context)));

        assertTrue(logOutput.contains("WARN"),
                "Expected WARN log for mixed votes, got: " + logOutput);
    }

    @Test
    void resolve_mixedVotes_publicRouteCheck_rejectsWithoutWarn() {
        NavigationContext context = NavigationContext.forPublicRouteCheck(
                mock(Router.class), AnonymousAllowedView.class,
                new Location("anon"), RouteParameters.empty());

        String logOutput = LogOutputCapture
                .capture(() -> assertEquals(AccessCheckDecision.REJECT,
                        resolveMixedVotes(context)));

        assertFalse(logOutput.contains("WARN"),
                "Public route check should not log WARN, got: " + logOutput);
    }

    private AccessCheckDecision resolveMixedVotes(NavigationContext context) {
        List<AccessCheckResult> results = List.of(context.allow(),
                context.deny("denied by test"));
        return resolver.resolve(results, context).decision();
    }
}
