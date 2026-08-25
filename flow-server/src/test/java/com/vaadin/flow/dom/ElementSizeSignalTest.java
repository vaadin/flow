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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Size;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ElementSizeSignalTest {

    @Test
    void sizeSignal_isReadOnlyAndCached() {
        UI ui = new MockUI();
        Element div = ElementFactory.createDiv();
        ui.getElement().appendChild(div);

        Signal<Size> signal = div.sizeSignal();

        assertFalse(signal instanceof ValueSignal,
                "sizeSignal() should return a read-only signal");
        assertEquals(new Size(0, 0), signal.peek());
        assertSame(signal, div.sizeSignal(),
                "sizeSignal() should return the same signal for an element");
    }

    @Test
    void sizeSignal_updatedByClientReportedSize() {
        UI ui = new MockUI();
        Element div = ElementFactory.createDiv();
        ui.getElement().appendChild(div);

        Signal<Size> signal = div.sizeSignal();

        reportSize(div, 800, 600);
        assertEquals(new Size(800, 600), signal.peek());

        reportSize(div, 1024, 768);
        assertEquals(new Size(1024, 768), signal.peek());
    }

    /**
     * Simulates the browser-side resize observer reporting a new size through
     * the return channel that the size trigger registered on the element.
     */
    private void reportSize(Element element, int width, int height) {
        ReturnChannelRegistration channel = element.getNode()
                .getFeature(ReturnChannelMap.class).get(0);

        ObjectNode size = JacksonUtils.createObjectNode();
        size.put("width", width);
        size.put("height", height);
        ArrayNode arguments = JacksonUtils.createArrayNode();
        arguments.add(size);

        channel.invoke(arguments);
    }
}
