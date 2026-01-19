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

import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class HasHelperBindHelperTextTest {

    private static MockVaadinServletService service;

    private MockedStatic<FeatureFlags> featureFlagStaticMock;

    private LinkedList<ErrorEvent> events;

    @BeforeClass
    public static void init() {
        var featureFlagStaticMock = mockStatic(FeatureFlags.class);
        featureFlagEnabled(featureFlagStaticMock);
        service = new MockVaadinServletService();
        close(featureFlagStaticMock);
    }

    @AfterClass
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Before
    public void before() {
        featureFlagStaticMock = mockStatic(FeatureFlags.class);
        featureFlagEnabled(featureFlagStaticMock);
        events = mockLockedSessionWithErrorHandler();
    }

    @After
    public void after() {
        close(featureFlagStaticMock);
        events = null;
    }

    private static void featureFlagEnabled(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        FeatureFlags flags = mock(FeatureFlags.class);
        when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                .thenReturn(true);
        featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                .thenReturn(flags);
    }

    private static void close(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        CurrentInstance.clearAll();
        featureFlagStaticMock.close();
    }

    private LinkedList<ErrorEvent> mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

    @Tag("div")
    public static class HasHelperComponent extends Component
            implements HasHelper {
    }

    @Test
    public void bindHelperText_updatesPropertyOnSignalChange() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("");
        c.bindHelperText(signal);

        signal.value("help-1");
        assertEquals("help-1", c.getElement().getProperty("helperText"));

        signal.value("help-2");
        assertEquals("help-2", c.getElement().getProperty("helperText"));

        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindHelperText_setHelperTextWhileBindingActive_throws() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        c.bindHelperText(signal);

        assertThrows(BindingActiveException.class,
                () -> c.setHelperText("manual"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindHelperText_unbindWithNull_stopsUpdates() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("a");
        c.bindHelperText(signal);
        assertEquals("a", c.getElement().getProperty("helperText"));

        c.bindHelperText(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals("a", c.getElement().getProperty("helperText"));
        Assert.assertTrue(events.isEmpty());
    }
}
