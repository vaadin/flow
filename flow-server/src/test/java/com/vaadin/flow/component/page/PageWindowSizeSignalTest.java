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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

public class PageWindowSizeSignalTest {

    @Test
    public void windowSizeSignal_isReadOnly() {
        Page page = new Page(new MockUI());
        Signal<WindowSize> signal = page.windowSizeSignal();
        Assert.assertFalse(
                "windowSizeSignal() should return a read-only signal",
                signal instanceof ValueSignal);
    }

    @Test
    public void windowSizeSignal_multipleResizes_tracksLatest() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        Signal<WindowSize> signal = page.windowSizeSignal();
        Assert.assertEquals(new WindowSize(0, 0), signal.peek());

        fireResizeEvent(ui, 1024, 768);
        Assert.assertEquals(new WindowSize(1024, 768), signal.peek());

        fireResizeEvent(ui, 1920, 1080);
        Assert.assertEquals(new WindowSize(1920, 1080), signal.peek());

        fireResizeEvent(ui, 800, 600);
        Assert.assertEquals(new WindowSize(800, 600), signal.peek());
    }

    @Test
    public void addBrowserWindowResizeListener_andWindowSizeSignal_shareJsSetup() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        // Add a resize listener first
        AtomicReference<BrowserWindowResizeEvent> listenerEvent = new AtomicReference<>();
        page.addBrowserWindowResizeListener(listenerEvent::set);

        // Then get the signal
        Signal<WindowSize> signal = page.windowSizeSignal();

        // Both should be updated by a single resize event
        fireResizeEvent(ui, 1280, 720);

        Assert.assertNotNull(listenerEvent.get());
        Assert.assertEquals(1280, listenerEvent.get().getWidth());
        Assert.assertEquals(720, listenerEvent.get().getHeight());
        Assert.assertEquals(new WindowSize(1280, 720), signal.peek());
    }

    @Test
    public void windowSizeSignal_thenAddBrowserWindowResizeListener_shareJsSetup() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        // Get the signal first
        Signal<WindowSize> signal = page.windowSizeSignal();

        // Then add a resize listener
        AtomicReference<BrowserWindowResizeEvent> listenerEvent = new AtomicReference<>();
        page.addBrowserWindowResizeListener(listenerEvent::set);

        // Both should be updated by a single resize event
        fireResizeEvent(ui, 1920, 1080);

        Assert.assertEquals(new WindowSize(1920, 1080), signal.peek());
        Assert.assertNotNull(listenerEvent.get());
        Assert.assertEquals(1920, listenerEvent.get().getWidth());
        Assert.assertEquals(1080, listenerEvent.get().getHeight());
    }

    private void fireResizeEvent(MockUI ui, int width, int height) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.w", width);
        eventData.put("event.h", height);
        // The resize listener uses .debounce(300) which sets TRAILING phase
        eventData.put(JsonConstants.EVENT_DATA_PHASE,
                JsonConstants.EVENT_PHASE_TRAILING);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(), "window-resize",
                        eventData));
    }
}
