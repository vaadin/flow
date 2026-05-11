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
package com.vaadin.flow.component.page;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class FullscreenSignalTest {

    @Test
    void fullscreenSignal_isReadOnly() {
        MockUI ui = new MockUI();
        Signal<FullscreenState> signal = ui.getPage().fullscreenSignal();
        assertFalse(signal instanceof ValueSignal,
                "fullscreenSignal() should return a read-only signal");
    }

    @Test
    void fullscreenSignal_defaultsToUnknownBeforeBootstrap() {
        MockUI ui = new MockUI();
        Signal<FullscreenState> signal = ui.getPage().fullscreenSignal();
        assertEquals(FullscreenState.UNKNOWN, signal.peek(),
                "Before bootstrap the value should be UNKNOWN so callers can "
                        + "distinguish 'no data yet' from a real value");
    }

    @Test
    void fullscreenSignal_readonlyWrapperIsCached() {
        Page page = new MockUI().getPage();
        assertSame(page.fullscreenSignal(), page.fullscreenSignal(),
                "Repeated calls must return the same read-only wrapper so "
                        + "subscriber identity stays stable");
    }

    @Test
    void fullscreenSignal_tracksStateChanges() {
        MockUI ui = new MockUI();
        Signal<FullscreenState> signal = ui.getPage().fullscreenSignal();

        fireFullscreenEvent(ui, "NOT_FULLSCREEN");
        assertEquals(FullscreenState.NOT_FULLSCREEN, signal.peek());

        fireFullscreenEvent(ui, "FULLSCREEN");
        assertEquals(FullscreenState.FULLSCREEN, signal.peek());

        fireFullscreenEvent(ui, "UNSUPPORTED");
        assertEquals(FullscreenState.UNSUPPORTED, signal.peek());
    }

    @Test
    void fullscreenSignal_unknownDetailKeepsPreviousValue() {
        MockUI ui = new MockUI();
        Signal<FullscreenState> signal = ui.getPage().fullscreenSignal();

        fireFullscreenEvent(ui, "FULLSCREEN");
        fireFullscreenEvent(ui, "SOMETHING_NEW");

        assertEquals(FullscreenState.FULLSCREEN, signal.peek(),
                "Unknown detail values from a newer client should not reset "
                        + "the signal");
    }

    private void fireFullscreenEvent(MockUI ui, String state) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail", state);
        // No debounce on the listener — DomEvent defaults to LEADING phase.
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-fullscreen-change", eventData));
    }
}
