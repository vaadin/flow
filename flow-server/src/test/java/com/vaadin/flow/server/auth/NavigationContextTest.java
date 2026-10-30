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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.NavigationContext.Phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NavigationContextTest {

    private final Router router = mock(Router.class);
    private final Location location = new Location("anon");

    @Test
    void forPublicRouteCheck_noUserAndPublicRouteCheckPhase() {
        NavigationContext context = NavigationContext.forPublicRouteCheck(
                router, AnonymousAllowedView.class, location,
                RouteParameters.empty());
        assertEquals(Phase.PUBLIC_ROUTE_CHECK, context.getPhase());
        assertNull(context.getPrincipal());
        assertFalse(context.hasRole("admin"));
        assertFalse(context.isNavigating());
        assertFalse(context.isErrorHandling());
    }

    @Test
    void constructor_publicRouteCheckPhase_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> create(Phase.PUBLIC_ROUTE_CHECK));
    }

    @Test
    void isNavigating_trueOnlyForNavigationPhase() {
        assertTrue(create(Phase.NAVIGATION).isNavigating());
        assertFalse(create(Phase.EVALUATION).isNavigating());
    }

    @Test
    @SuppressWarnings("removal")
    void deprecatedNavigatingConstructor_mapsToPhase() {
        assertEquals(Phase.NAVIGATION,
                new NavigationContext(router, AnonymousAllowedView.class,
                        location, RouteParameters.empty(), null, role -> false,
                        false, true).getPhase());
        assertEquals(Phase.EVALUATION,
                new NavigationContext(router, AnonymousAllowedView.class,
                        location, RouteParameters.empty(), null, role -> false,
                        false, false).getPhase());
    }

    private NavigationContext create(Phase phase) {
        return new NavigationContext(router, AnonymousAllowedView.class,
                location, RouteParameters.empty(), null, role -> false, false,
                phase);
    }
}
