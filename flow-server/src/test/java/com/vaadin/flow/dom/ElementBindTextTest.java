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
package com.vaadin.flow.dom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ElementBindTextTest {

    private static MockVaadinServletService service;

    private MockedStatic<FeatureFlags> featureFlagStaticMock;

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
        VaadinService.setCurrent(service);
        VaadinSession session = new MockVaadinSession(service);
        session.lock();
        new MockUI(session);
    }

    @After
    public void after() {
        close(featureFlagStaticMock);
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

    @Test
    public void bindText_getText_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());
    }

    @Test
    public void bindText_detachAttach_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());
        UI.getCurrent().getElement().removeChild(element);
        signal.value("text2");
        assertEquals("text", element.getText());
        UI.getCurrent().getElement().appendChild(element);
        assertEquals("text2", element.getText());
    }

    @Test
    public void bindText_setTextWithActiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertThrows(BindingActiveException.class,
                () -> element.setText("text2"));
    }

    @Test
    public void bindText_componentNotAttached_bindingIgnored() {
        Element element = new Element("span");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        element.bindText(signal);

        assertEquals("", element.getText());
    }
}
