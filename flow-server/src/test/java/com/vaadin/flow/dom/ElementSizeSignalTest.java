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
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ElementSizeSignalTest {

    @Tag(Tag.DIV)
    private static class TestDiv extends Component {
    }

    @Test
    void sizeSignal_isReadOnly() {
        MockUI ui = new MockUI();
        TestDiv div = new TestDiv();
        ui.add(div);

        Signal<Element.Size> signal = div.getElement().sizeSignal();
        assertFalse(signal instanceof ValueSignal,
                "sizeSignal() should return a read-only signal");
    }

    @Test
    void sizeSignal_defaultValue_isZeroByZero() {
        MockUI ui = new MockUI();
        TestDiv div = new TestDiv();
        ui.add(div);

        Signal<Element.Size> signal = div.getElement().sizeSignal();
        assertEquals(new Element.Size(0, 0), signal.peek());
    }

    @Test
    void sizeSignal_returnsSameInstance() {
        MockUI ui = new MockUI();
        TestDiv div = new TestDiv();
        ui.add(div);

        Signal<Element.Size> first = div.getElement().sizeSignal();
        Signal<Element.Size> second = div.getElement().sizeSignal();
        // Both wrappers read from the same underlying ValueSignal
        fireElementResizeEvent(ui, 0, 640, 480);
        assertEquals(new Element.Size(640, 480), first.peek());
        assertEquals(new Element.Size(640, 480), second.peek());
    }

    @Test
    void sizeSignal_updatesOnResizeEvent() {
        MockUI ui = new MockUI();
        TestDiv div = new TestDiv();
        ui.add(div);

        Signal<Element.Size> signal = div.getElement().sizeSignal();
        assertEquals(new Element.Size(0, 0), signal.peek());

        fireElementResizeEvent(ui, 0, 800, 600);
        assertEquals(new Element.Size(800, 600), signal.peek());

        fireElementResizeEvent(ui, 0, 1024, 768);
        assertEquals(new Element.Size(1024, 768), signal.peek());
    }

    private void fireElementResizeEvent(MockUI ui, int elementId, int width,
            int height) {
        ObjectNode eventData = JacksonUtils.createObjectNode();

        ObjectNode sizes = JacksonUtils.createObjectNode();
        ObjectNode sizeEntry = JacksonUtils.createObjectNode();
        sizeEntry.put("w", width);
        sizeEntry.put("h", height);
        sizes.set(String.valueOf(elementId), sizeEntry);

        eventData.set("event.sizes", sizes);
        eventData.put(JsonConstants.EVENT_DATA_PHASE,
                JsonConstants.EVENT_PHASE_TRAILING);

        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-component-resize", eventData));
    }
}
