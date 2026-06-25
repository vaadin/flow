/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.hotswap.impl;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

public class ErrorViewHotswapperTest {

    private ErrorViewHotswapper hotswapper;
    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @Before
    public void setUp() {
        CurrentInstance.clearAll();
        service = new MockVaadinServletService();
        session = new AlwaysLockedVaadinSession(service);
        session.setConfiguration(service.getDeploymentConfiguration());
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
    public void onClassesChange_errorViewShown_redefined_triggersRefresh() {
        // Simulate an error view being displayed
        TestErrorView errorView = new TestErrorView();
        ui.getInternals().showRouteTarget(createMockLocation("error"),
                errorView, Collections.emptyList());

        // Verify error view is actually showing
        Assert.assertTrue("Error view should be showing",
                ui.getInternals().isShowingErrorView());

        // Simulate a class being redefined (hotswap)
        boolean reload = hotswapper.onClassLoadEvent(session,
                Set.of(String.class), true);

        // Verify refresh was triggered
        Assert.assertTrue("Should trigger refresh when error view is shown",
                reload);
    }

    @Test
    public void onClassesChange_normalViewShown_redefined_noRefresh() {
        // Simulate a normal view being displayed
        TestNormalView normalView = new TestNormalView();
        ui.getInternals().showRouteTarget(createMockLocation("normal"),
                normalView, Collections.emptyList());

        // Verify error view is not showing
        Assert.assertFalse("Normal view should not be an error view",
                ui.getInternals().isShowingErrorView());

        // Simulate a class being redefined (hotswap)
        boolean reload = hotswapper.onClassLoadEvent(session,
                Set.of(String.class), true);

        // Verify refresh was not triggered
        Assert.assertFalse("Should not trigger refresh for normal view",
                reload);
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
