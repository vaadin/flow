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
package com.vaadin.base.devserver.hotswap.impl;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorViewHotswapperTest {

    private ErrorViewHotswapper hotswapper;
    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @BeforeEach
    void setUp() {
        CurrentInstance.clearAll();
        service = new MockVaadinServletService();
        session = new AlwaysLockedVaadinSession(service);
        ui = new MockUI(session);
        ui.doInit(null, 42, "test");
        session.addUI(ui);
        hotswapper = new ErrorViewHotswapper();
    }

    private Location createMockLocation(String path) {
        Location location = Mockito.mock(Location.class);
        Mockito.when(location.getPath()).thenReturn(path);
        return location;
    }

    @Test
    void onClassesChange_errorViewShown_redefined_triggersRefresh() {
        // Simulate an error view being displayed
        TestErrorView errorView = new TestErrorView();
        ui.getInternals().showRouteTarget(createMockLocation("error"),
                errorView, Collections.emptyList());

        // Verify error view is actually showing
        assertTrue(ui.getInternals().isShowingErrorView(),
                "Error view should be showing");

        // Simulate a class being redefined (hotswap)
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(String.class), true);
        hotswapper.onClassesChange(event);

        // Verify refresh was triggered
        assertEquals(UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null),
                "Should trigger refresh when error view is shown");
    }

    @Test
    void onClassesChange_normalViewShown_redefined_noRefresh() {
        // Simulate a normal view being displayed
        TestNormalView normalView = new TestNormalView();
        ui.getInternals().showRouteTarget(createMockLocation("normal"),
                normalView, Collections.emptyList());

        // Verify error view is not showing
        assertFalse(ui.getInternals().isShowingErrorView(),
                "Normal view should not be an error view");

        // Simulate a class being redefined (hotswap)
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(String.class), true);
        hotswapper.onClassesChange(event);

        // Verify refresh was not triggered
        assertFalse(event.getUIUpdateStrategy(ui).isPresent(),
                "Should not trigger refresh for normal view");
    }

    // Test classes

    @Tag("div")
    public static class TestErrorView extends Component
            implements HasErrorParameter<Exception> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<Exception> parameter) {
            return 500;
        }
    }

    @Tag("div")
    public static class TestNormalView extends Component {
    }
}
