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
package com.vaadin.flow.dom;

import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.TextBindingFeature;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.SharedValueSignal;
import com.vaadin.signals.Signal;
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

        var ui = new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

    @Test
    public void bindTextComputedSignal_getText_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.value());
        element.bindText(computedSignal);

        assertEquals("computed-text", element.getText());
    }

    @Test
    public void bindTextMappedSignal_getText_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal.map(text -> "mapped-" + text));

        assertEquals("mapped-text", element.getText());
    }

    @Test
    public void bindText_detachAttach_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());
        UI.getCurrent().getElement().removeChild(element);
        signal.value("text2");
        assertEquals("text", element.getText());
        UI.getCurrent().getElement().appendChild(element);
        assertEquals("text2", element.getText());
    }

    @Test
    public void bindText_setTextWithExistingActiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        assertThrows(BindingActiveException.class,
                () -> element.setText("text2"));
    }

    @Test
    public void bindText_setTextWithExistingInactiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        UI.getCurrent().getElement().removeChild(element);
        assertThrows(BindingActiveException.class,
                () -> element.setText("text2"));
    }

    @Test
    public void bindText_initialNullSignalValue_treatAsBlank() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        element.bindText(signal);
        assertEquals("", element.getText());
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindText_setNullSignalValue_treatAsBlank() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);
        signal.value(null);
        Assert.assertTrue(events.isEmpty());
        assertEquals("", element.getText());
    }

    @Test
    public void bindText_bindTextWithExistingActiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        SharedValueSignal<String> signal2 = new SharedValueSignal<>("text2");
        assertThrows(BindingActiveException.class,
                () -> element.bindText(signal2));
    }

    @Test
    public void bindText_bindTextWithExistingInactiveBinding_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        UI.getCurrent().getElement().removeChild(element);
        SharedValueSignal<String> signal2 = new SharedValueSignal<>("text2");
        element.bindText(signal2);
    }

    @Test
    public void bindText_unbindText_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");

        element.bindText(signal);
        element.bindText(null);

        assertEquals("text", element.getText());
    }

    @Test
    public void bindText_unbindText_allowsSetText() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");

        element.bindText(signal);
        element.bindText(null);

        element.setText("text2");
        assertEquals("text2", element.getText());
    }

    @Test
    public void bindText_componentNotAttached_bindingIgnored() {
        Element element = new Element("span");
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        assertEquals("", element.getText());
    }

    @Test
    public void bindText_componentAttached_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());

        signal.value("text2");
        assertEquals("text2", element.getText());
    }

    @Test
    public void lazyInitSignalBindingFeature() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        element.setText("text2");
        element.getText();

        element.getNode().getFeatureIfInitialized(TextBindingFeature.class)
                .ifPresent(feature -> Assert.fail(
                        "TextBindingFeature should not be initialized before binding a signal"));

        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        element.bindText(signal);

        element.getNode().getFeatureIfInitialized(TextBindingFeature.class)
                .orElseThrow(() -> new AssertionError(
                        "TextBindingFeature should be initialized after binding a signal"));
    }

    /*
     * HasText interface's default bindText should delegate to Element's
     * bindText. This test verifies that with a custom Span component.
     */
    @Test
    public void bindText_componentWithHasText() {
        @Tag(Tag.SPAN)
        class SpanWithHasText extends Component implements HasText {
        }

        SpanWithHasText span = new SpanWithHasText();
        UI.getCurrent().add(span);

        SharedValueSignal<String> signal = new SharedValueSignal<>("text");
        span.bindText(signal);
        assertEquals("text", span.getText());

        signal.value("text2");
        assertEquals("text2", span.getText());

        // verify text is blank with null signal value
        signal.value(null);
        assertEquals("", span.getText());

        // verify setText throws with active binding
        Assert.assertThrows(BindingActiveException.class,
                () -> span.setText(""));

        // detach
        UI.getCurrent().remove(span);
        signal.value("text3");
        assertEquals("", span.getText());
        // reattach
        UI.getCurrent().add(span);
        assertEquals("text3", span.getText());

        // unbind and verify setText works
        span.bindText(null);
        span.setText("text");
        assertEquals("text", span.getText());
    }
}
