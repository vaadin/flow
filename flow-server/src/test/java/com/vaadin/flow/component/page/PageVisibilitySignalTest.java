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
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PageVisibilitySignalTest {

    @Test
    void pageVisibilitySignal_isReadOnly() {
        Page page = new Page(new MockUI());
        Signal<PageVisibility> signal = page.pageVisibilitySignal();
        assertFalse(signal instanceof ValueSignal,
                "pageVisibilitySignal() should return a read-only signal");
    }

    @Test
    void pageVisibilitySignal_defaultsToVisible() {
        Page page = new Page(new MockUI());
        Signal<PageVisibility> signal = page.pageVisibilitySignal();
        assertEquals(PageVisibility.VISIBLE, signal.get());
    }

    @Test
    void pageVisibilitySignal_tracksVisibilityChanges() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        Signal<PageVisibility> signal = page.pageVisibilitySignal();
        assertEquals(PageVisibility.VISIBLE, signal.get());

        fireVisibilityEvent(ui, "HIDDEN");
        assertEquals(PageVisibility.HIDDEN, signal.get());

        fireVisibilityEvent(ui, "VISIBLE");
        assertEquals(PageVisibility.VISIBLE, signal.get());

        fireVisibilityEvent(ui, "VISIBLE_NOT_FOCUSED");
        assertEquals(PageVisibility.VISIBLE_NOT_FOCUSED, signal.get());
    }

    private void fireVisibilityEvent(MockUI ui, String visibility) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail", visibility);
        eventData.put(JsonConstants.EVENT_DATA_PHASE,
                JsonConstants.EVENT_PHASE_TRAILING);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-page-visibility-change", eventData));
    }
}
