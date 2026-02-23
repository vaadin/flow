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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ElementBindTextTest {

    private static MockVaadinServletService service;

    private LinkedList<ErrorEvent> events;

    @BeforeAll
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterAll
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @BeforeEach
    public void before() {
        events = mockLockedSessionWithErrorHandler();
    }

    @AfterEach
    public void after() {
        CurrentInstance.clearAll();
        events = null;
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

        ValueSignal<String> signal = new ValueSignal<>("text");
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.get());
        element.bindText(computedSignal);

        assertEquals("computed-text", element.getText());
    }

    @Test
    public void bindTextMappedSignal_getText_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal.map(text -> "mapped-" + text));

        assertEquals("mapped-text", element.getText());
    }

    @Test
    public void bindText_detachAttach_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());
        UI.getCurrent().getElement().removeChild(element);
        signal.set("text2");
        assertEquals("text", element.getText());
        UI.getCurrent().getElement().appendChild(element);
        assertEquals("text2", element.getText());
    }

    @Test
    public void bindText_setTextWithExistingActiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertThrows(BindingActiveException.class,
                () -> element.setText("text2"));
    }

    @Test
    public void bindText_setTextWithExistingInactiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        UI.getCurrent().getElement().removeChild(element);
        assertThrows(BindingActiveException.class,
                () -> element.setText("text2"));
    }

    @Test
    public void bindText_initialNullSignalValue_treatAsBlank() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<@Nullable String> signal = new ValueSignal<>(null);
        element.bindText(signal);
        assertEquals("", element.getText());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindText_setNullSignalValue_treatAsBlank() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);
        signal.set(null);
        assertTrue(events.isEmpty());
        assertEquals("", element.getText());
    }

    @Test
    public void bindText_bindTextWithExistingActiveBinding_throws() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        ValueSignal<String> signal2 = new ValueSignal<>("text2");
        assertThrows(BindingActiveException.class,
                () -> element.bindText(signal2));
    }

    @Test
    public void bindText_bindTextWithExistingInactiveBinding_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        UI.getCurrent().getElement().removeChild(element);
        ValueSignal<String> signal2 = new ValueSignal<>("text2");
        element.bindText(signal2);
    }

    @Test
    public void bindText_removeBindingViaFeature_stopsUpdatesAndAllowsManualSet() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);
        assertEquals("text", element.getText());

        // Remove binding via the node's TextBindingFeature
        TextBindingFeature feature = element.getNode()
                .getFeature(TextBindingFeature.class);
        feature.removeBinding();

        // Signal changes should no longer affect the element
        signal.set("text2");
        assertEquals("text", element.getText());

        // Manual set should work without throwing
        element.setText("manual");
        assertEquals("manual", element.getText());
    }

    @Test
    public void bindText_nullSignal_throwsNPE() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        assertThrows(NullPointerException.class, () -> element.bindText(null));
    }

    @Test
    public void bindText_componentNotAttached_bindingIgnored() {
        Element element = new Element("span");
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertEquals("", element.getText());
    }

    @Test
    public void bindText_componentAttached_returnsCorrectValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<String> signal = new ValueSignal<>("text");
        element.bindText(signal);

        assertEquals("text", element.getText());

        signal.set("text2");
        assertEquals("text2", element.getText());
    }

    @Test
    public void lazyInitSignalBindingFeature() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        element.setText("text2");
        element.getText();

        element.getNode().getFeatureIfInitialized(TextBindingFeature.class)
                .ifPresent(feature -> fail(
                        "TextBindingFeature should not be initialized before binding a signal"));

        ValueSignal<String> signal = new ValueSignal<>("text");
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

        ValueSignal<String> signal = new ValueSignal<>("text");
        span.bindText(signal);
        assertEquals("text", span.getText());

        signal.set("text2");
        assertEquals("text2", span.getText());

        // verify text is blank with null signal value
        signal.set(null);
        assertEquals("", span.getText());

        // verify setText throws with active binding
        assertThrows(BindingActiveException.class, () -> span.setText(""));

        // detach
        UI.getCurrent().remove(span);
        signal.set("text3");
        assertEquals("", span.getText());
        // reattach
        UI.getCurrent().add(span);
        assertEquals("text3", span.getText());
    }

    @Test
    public void bindText_hasText_nullSignal_throwsNPE() {
        @Tag(Tag.SPAN)
        class SpanWithHasText extends Component implements HasText {
        }

        SpanWithHasText span = new SpanWithHasText();
        UI.getCurrent().add(span);

        assertThrows(NullPointerException.class, () -> span.bindText(null));
    }
}
