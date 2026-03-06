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

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageScreenOrientationTest {

    @Test
    void lockOrientation_executesCorrectJs() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        page.lockOrientation(ScreenOrientation.LANDSCAPE_PRIMARY);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.lock")));
    }

    @Test
    void unlockOrientation_executesCorrectJs() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        page.unlockOrientation();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.unlock()")));
    }

    @Test
    void screenOrientationSignal_returnsReadOnlySignal() {
        Page page = new Page(new MockUI());
        Signal<ScreenOrientationData> signal = page.screenOrientationSignal();
        assertFalse(signal instanceof ValueSignal,
                "screenOrientationSignal() should return a read-only signal");
    }

    @Test
    void screenOrientationSignal_updatedOnOrientationChange() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        Signal<ScreenOrientationData> signal = page.screenOrientationSignal();
        assertEquals(ScreenOrientation.PORTRAIT_PRIMARY, signal.get().type());
        assertEquals(0, signal.get().angle());

        fireOrientationChangeEvent(ui, "landscape-primary", 90);
        assertEquals(ScreenOrientation.LANDSCAPE_PRIMARY, signal.get().type());
        assertEquals(90, signal.get().angle());
    }

    @Test
    void screenOrientationData_recordAccessors() {
        ScreenOrientationData data = new ScreenOrientationData(
                ScreenOrientation.LANDSCAPE_SECONDARY, 270);
        assertEquals(ScreenOrientation.LANDSCAPE_SECONDARY, data.type());
        assertEquals(270, data.angle());
    }

    @Test
    void screenOrientation_fromClientValue() {
        assertEquals(ScreenOrientation.PORTRAIT_PRIMARY,
                ScreenOrientation.fromClientValue("portrait-primary"));
        assertEquals(ScreenOrientation.PORTRAIT_SECONDARY,
                ScreenOrientation.fromClientValue("portrait-secondary"));
        assertEquals(ScreenOrientation.LANDSCAPE_PRIMARY,
                ScreenOrientation.fromClientValue("landscape-primary"));
        assertEquals(ScreenOrientation.LANDSCAPE_SECONDARY,
                ScreenOrientation.fromClientValue("landscape-secondary"));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenOrientation.fromClientValue("unknown"));
    }

    private void fireOrientationChangeEvent(MockUI ui, String type, int angle) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.orientationType", type);
        eventData.put("event.orientationAngle", angle);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-orientation-change", eventData));
    }
}
