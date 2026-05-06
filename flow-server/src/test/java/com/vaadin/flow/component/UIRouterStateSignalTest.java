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
package com.vaadin.flow.component;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.signals.Signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UI#routerStateSignal()}.
 */
class UIRouterStateSignalTest extends SignalsUnitTest {

    @Tag("test-view")
    private static class TestView extends Component {
    }

    @Test
    void routerStateSignal_returnsCachedReadonlyInstance() {
        UI ui = UI.getCurrent();
        Signal<RouterState> a = ui.routerStateSignal();
        Signal<RouterState> b = ui.routerStateSignal();

        assertNotNull(a, "routerStateSignal() should never return null");
        assertSame(a, b,
                "Repeated calls must return the same cached read-only signal");
    }

    @Test
    void routerStateSignal_initialValueHasEmptyChain() {
        UI ui = UI.getCurrent();
        RouterState initial = ui.routerStateSignal().peek();

        assertNotNull(initial);
        assertEquals("", initial.location().getPath());
        assertTrue(initial.activeChain().isEmpty());
        assertTrue(initial.currentView().isEmpty());
        assertTrue(initial.routeParameters().getParameterNames().isEmpty());
    }

    @Test
    void routerStateSignal_updatedAfterUpdateRouterState() {
        UI ui = UI.getCurrent();
        Signal<RouterState> signal = ui.routerStateSignal();

        TestView leaf = new TestView();
        RouterState newState = new RouterState(new Location("foo/bar"),
                RouteParameters.empty(), List.of(leaf), TestView.class);

        ui.getInternals().updateRouterState(newState);

        RouterState observed = signal.peek();
        assertEquals("foo/bar", observed.location().getPath());
        assertSame(leaf, observed.currentView().orElseThrow());
        assertEquals(TestView.class, observed.navigationTarget());
    }

    @Test
    void routerStateSignal_consecutiveUpdatesArePropagated() {
        UI ui = UI.getCurrent();
        Signal<RouterState> signal = ui.routerStateSignal();

        ui.getInternals().updateRouterState(new RouterState(new Location("a"),
                RouteParameters.empty(), Collections.emptyList(), null));
        assertEquals("a", signal.peek().location().getPath());

        ui.getInternals().updateRouterState(new RouterState(new Location("b"),
                RouteParameters.empty(), Collections.emptyList(), null));
        assertEquals("b", signal.peek().location().getPath());
    }
}
