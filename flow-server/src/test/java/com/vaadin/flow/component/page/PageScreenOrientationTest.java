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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PageScreenOrientationTest {

    @Test
    void screenOrientationSignal_isReadOnly() {
        MockUI ui = new MockUI();
        Signal<ScreenOrientationData> signal = ui.getPage()
                .screenOrientationSignal();
        assertFalse(signal instanceof ValueSignal,
                "screenOrientationSignal() should return a read-only signal");
    }

    @Test
    void screenOrientationSignal_defaultsToUnknownBeforeBootstrap() {
        MockUI ui = new MockUI();
        Signal<ScreenOrientationData> signal = ui.getPage()
                .screenOrientationSignal();
        assertEquals(ScreenOrientation.UNKNOWN, signal.peek().type(),
                "Before bootstrap the type should be UNKNOWN so callers can "
                        + "distinguish 'no data yet' from a real value");
        assertEquals(0, signal.peek().angle());
    }

    @Test
    void screenOrientationSignal_readonlyWrapperIsCached() {
        Page page = new MockUI().getPage();
        assertSame(page.screenOrientationSignal(),
                page.screenOrientationSignal(),
                "Repeated calls must return the same read-only wrapper so "
                        + "subscriber identity stays stable");
    }

    @Test
    void screenOrientationSignal_tracksOrientationChanges() {
        MockUI ui = new MockUI();
        Signal<ScreenOrientationData> signal = ui.getPage()
                .screenOrientationSignal();

        fireOrientationEvent(ui, "landscape-primary", 90);
        assertEquals(ScreenOrientation.LANDSCAPE_PRIMARY, signal.peek().type());
        assertEquals(90, signal.peek().angle());

        fireOrientationEvent(ui, "portrait-secondary", 180);
        assertEquals(ScreenOrientation.PORTRAIT_SECONDARY,
                signal.peek().type());
        assertEquals(180, signal.peek().angle());
    }

    @Test
    void screenOrientationSignal_unknownTypeKeepsPreviousValue() {
        MockUI ui = new MockUI();
        Signal<ScreenOrientationData> signal = ui.getPage()
                .screenOrientationSignal();

        fireOrientationEvent(ui, "landscape-primary", 90);
        fireOrientationEvent(ui, "diagonal-future", 45);

        assertEquals(ScreenOrientation.LANDSCAPE_PRIMARY, signal.peek().type(),
                "Unknown type values from a newer client should not reset "
                        + "the signal");
        assertEquals(90, signal.peek().angle());
    }

    @Test
    void setScreenOrientation_fromBootstrapSeedsSignal() {
        MockUI ui = new MockUI();
        ui.getPage().setScreenOrientation("landscape-secondary", "270");

        ScreenOrientationData data = ui.getPage().screenOrientationSignal()
                .peek();
        assertEquals(ScreenOrientation.LANDSCAPE_SECONDARY, data.type());
        assertEquals(270, data.angle());
    }

    @Test
    void setScreenOrientation_emptyTypeIsIgnored() {
        MockUI ui = new MockUI();
        ui.getPage().setScreenOrientation("", "0");
        assertEquals(ScreenOrientation.UNKNOWN,
                ui.getPage().screenOrientationSignal().peek().type(),
                "Empty type from a browser without the Screen Orientation API "
                        + "must keep UNKNOWN, not crash");
    }

    @Test
    void lockOrientation_executesCorrectJs() {
        MockUI ui = new MockUI();
        ui.getPage().lockOrientation(ScreenOrientation.LANDSCAPE_PRIMARY);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.lock(")));
    }

    @Test
    void lockOrientation_unknownIsRejected() {
        Page page = new MockUI().getPage();
        assertThrows(IllegalArgumentException.class,
                () -> page.lockOrientation(ScreenOrientation.UNKNOWN));
    }

    @Test
    void lockOrientation_unsupportedIsRejected() {
        Page page = new MockUI().getPage();
        assertThrows(IllegalArgumentException.class,
                () -> page.lockOrientation(ScreenOrientation.UNSUPPORTED));
    }

    @Test
    void lockOrientation_successCallbackFires() {
        MockUI ui = new MockUI();
        AtomicBoolean success = new AtomicBoolean();
        ui.getPage().lockOrientation(ScreenOrientation.LANDSCAPE_PRIMARY,
                () -> success.set(true),
                error -> fail("onError should not fire: " + error.message()));

        ObjectNode result = JacksonUtils.createObjectNode();
        result.put("success", true);
        resolveLockPromise(ui, result);

        assertTrue(success.get(),
                "onSuccess must fire when the client resolves with success");
    }

    @Test
    void lockOrientation_errorCallbackFires() {
        MockUI ui = new MockUI();
        AtomicReference<ScreenOrientationLockError> captured = new AtomicReference<>();
        ui.getPage().lockOrientation(ScreenOrientation.LANDSCAPE_PRIMARY,
                () -> fail("onSuccess should not fire"), captured::set);

        ObjectNode result = JacksonUtils.createObjectNode();
        result.put("success", false);
        result.put("name", "SecurityError");
        result.put("message", "Must be in fullscreen");
        resolveLockPromise(ui, result);

        assertEquals("SecurityError", captured.get().name());
        assertEquals("Must be in fullscreen", captured.get().message());
    }

    @Test
    void unlockOrientation_executesCorrectJs() {
        MockUI ui = new MockUI();
        ui.getPage().unlockOrientation();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.unlock()")));
    }

    @Test
    void screenOrientation_fromClientValue() {
        assertEquals(ScreenOrientation.PORTRAIT_PRIMARY,
                ScreenOrientation.fromClientValue("portrait-primary"));
        assertEquals(ScreenOrientation.LANDSCAPE_SECONDARY,
                ScreenOrientation.fromClientValue("landscape-secondary"));
        assertEquals(ScreenOrientation.UNSUPPORTED,
                ScreenOrientation.fromClientValue("unsupported"));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenOrientation.fromClientValue("diagonal-future"));
    }

    @Test
    void isLandscape_isPortrait() {
        assertTrue(ScreenOrientation.LANDSCAPE_PRIMARY.isLandscape());
        assertTrue(ScreenOrientation.LANDSCAPE_SECONDARY.isLandscape());
        assertFalse(ScreenOrientation.PORTRAIT_PRIMARY.isLandscape());
        assertFalse(ScreenOrientation.UNKNOWN.isLandscape());
        assertFalse(ScreenOrientation.UNSUPPORTED.isLandscape());

        assertTrue(ScreenOrientation.PORTRAIT_PRIMARY.isPortrait());
        assertTrue(ScreenOrientation.PORTRAIT_SECONDARY.isPortrait());
        assertFalse(ScreenOrientation.LANDSCAPE_PRIMARY.isPortrait());
        assertFalse(ScreenOrientation.UNKNOWN.isPortrait());
        assertFalse(ScreenOrientation.UNSUPPORTED.isPortrait());
    }

    @Test
    void setScreenOrientation_unsupportedFromBootstrap() {
        MockUI ui = new MockUI();
        ui.getPage().setScreenOrientation("unsupported", "0");
        assertEquals(ScreenOrientation.UNSUPPORTED,
                ui.getPage().screenOrientationSignal().peek().type(),
                "Client-side 'unsupported' must be observable distinctly "
                        + "from the pre-bootstrap UNKNOWN state");
    }

    private static void resolveLockPromise(MockUI ui, ObjectNode result) {
        PendingJavaScriptInvocation invocation = ui.dumpPendingJsInvocations()
                .stream()
                .filter(inv -> inv.getInvocation().getExpression()
                        .contains("screenOrientation.lock"))
                .reduce((a, b) -> b).orElseThrow();
        invocation.complete(result);
    }

    private void fireOrientationEvent(MockUI ui, String type, int angle) {
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("type", type);
        detail.put("angle", angle);
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.set("event.detail", detail);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-screen-orientation-change", eventData));
    }
}
