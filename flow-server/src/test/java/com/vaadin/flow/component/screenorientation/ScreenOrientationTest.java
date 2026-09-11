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
package com.vaadin.flow.component.screenorientation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.ExtendedClientDetails;
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

class ScreenOrientationTest {

    private MockUI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void orientationSignal_isReadOnly() {
        Signal<ScreenOrientationData> signal = ScreenOrientation
                .orientationSignal();
        assertFalse(signal instanceof ValueSignal,
                "orientationSignal() should return a read-only signal");
    }

    @Test
    void orientationSignal_defaultsToUnknownBeforeBootstrap() {
        Signal<ScreenOrientationData> signal = ScreenOrientation
                .orientationSignal();
        assertEquals(ScreenOrientationType.UNKNOWN, signal.peek().type(),
                "Before bootstrap the type should be UNKNOWN so callers can "
                        + "distinguish 'no data yet' from a real value");
        assertEquals(0, signal.peek().angle());
    }

    @Test
    void orientationSignal_readonlyWrapperIsCached() {
        assertSame(ScreenOrientation.orientationSignal(),
                ScreenOrientation.orientationSignal(),
                "Repeated calls must return the same read-only wrapper so "
                        + "subscriber identity stays stable");
    }

    @Test
    void orientationSignal_tracksOrientationChanges() {
        Signal<ScreenOrientationData> signal = ScreenOrientation
                .orientationSignal();

        fireOrientationEvent(ui, "landscape-primary", 90);
        assertEquals(ScreenOrientationType.LANDSCAPE_PRIMARY,
                signal.peek().type());
        assertEquals(90, signal.peek().angle());

        fireOrientationEvent(ui, "portrait-secondary", 180);
        assertEquals(ScreenOrientationType.PORTRAIT_SECONDARY,
                signal.peek().type());
        assertEquals(180, signal.peek().angle());
    }

    @Test
    void orientationSignal_unknownTypeKeepsPreviousValue() {
        Signal<ScreenOrientationData> signal = ScreenOrientation
                .orientationSignal();

        fireOrientationEvent(ui, "landscape-primary", 90);
        fireOrientationEvent(ui, "diagonal-future", 45);

        assertEquals(ScreenOrientationType.LANDSCAPE_PRIMARY,
                signal.peek().type(),
                "Unknown type values from a newer client should not reset "
                        + "the signal");
        assertEquals(90, signal.peek().angle());
    }

    @Test
    void setStateFromClient_fromBootstrapSeedsSignal() {
        ui.getInternals().setScreenOrientationFromClient("landscape-secondary",
                "270");

        ScreenOrientationData data = ScreenOrientation.orientationSignal()
                .peek();
        assertEquals(ScreenOrientationType.LANDSCAPE_SECONDARY, data.type());
        assertEquals(270, data.angle());
    }

    @Test
    void setStateFromClient_emptyTypeIsIgnored() {
        ui.getInternals().setScreenOrientationFromClient("", "0");
        assertEquals(ScreenOrientationType.UNKNOWN,
                ScreenOrientation.orientationSignal().peek().type(),
                "Empty type from a browser without the Screen Orientation API "
                        + "must keep UNKNOWN, not crash");
    }

    @Test
    void setStateFromClient_unparseableAngleStillAppliesType() {
        ui.getInternals().setScreenOrientationFromClient("landscape-primary",
                "abc");

        ScreenOrientationData data = ScreenOrientation.orientationSignal()
                .peek();
        assertEquals(ScreenOrientationType.LANDSCAPE_PRIMARY, data.type(),
                "A valid type must still be applied when the angle is "
                        + "unparseable");
        assertEquals(0, data.angle(), "An unparseable angle falls back to 0");
    }

    @Test
    void lock_executesCorrectJs() {
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.lock(")));
    }

    @Test
    void lock_unknownIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ScreenOrientation.lock(ScreenOrientationType.UNKNOWN));
    }

    @Test
    void lock_unsupportedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScreenOrientation
                .lock(ScreenOrientationType.UNSUPPORTED));
    }

    @Test
    void lock_successCallbackFires() {
        AtomicBoolean success = new AtomicBoolean();
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY,
                () -> success.set(true),
                error -> fail("onError should not fire: " + error.debugInfo()));

        ObjectNode result = JacksonUtils.createObjectNode();
        result.put("success", true);
        resolveLockPromise(ui, result);

        assertTrue(success.get(),
                "onSuccess must fire when the client resolves with success");
    }

    @Test
    void lock_errorCallbackFires() {
        AtomicReference<ScreenOrientationLockError> captured = new AtomicReference<>();
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY,
                () -> fail("onSuccess should not fire"), captured::set);

        ObjectNode result = JacksonUtils.createObjectNode();
        result.put("success", false);
        result.put("code", "SECURITY");
        result.put("message", "Document is hidden");
        resolveLockPromise(ui, result);

        assertEquals(ScreenOrientationLockErrorCode.SECURITY,
                captured.get().errorCode(),
                "The typed code sent by the client must deserialize into the "
                        + "error code enum");
        assertEquals("Document is hidden", captured.get().debugInfo());
    }

    @Test
    void lock_errorWithoutCodeFallsBackToUnknown() {
        AtomicReference<ScreenOrientationLockError> captured = new AtomicReference<>();
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY,
                () -> fail("onSuccess should not fire"), captured::set);

        ObjectNode result = JacksonUtils.createObjectNode();
        result.put("success", false);
        resolveLockPromise(ui, result);

        assertEquals(ScreenOrientationLockErrorCode.UNKNOWN,
                captured.get().errorCode(),
                "A missing code must surface as UNKNOWN, not null");
        assertEquals("", captured.get().debugInfo());
    }

    @Test
    void unlock_executesCorrectJs() {
        ScreenOrientation.unlock();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "window.Vaadin.Flow.screenOrientation.unlock()")));
    }

    @Test
    void unlock_completionCallbackFires() {
        AtomicBoolean done = new AtomicBoolean();
        ScreenOrientation.unlock(() -> done.set(true));

        PendingJavaScriptInvocation invocation = ui.dumpPendingJsInvocations()
                .stream()
                .filter(inv -> inv.getInvocation().getExpression()
                        .contains("screenOrientation.unlock"))
                .reduce((a, b) -> b).orElseThrow();
        invocation.complete(JacksonUtils.nullNode());

        assertTrue(done.get(),
                "onComplete must fire once the unlock round-trip resolves");
    }

    @Test
    void screenOrientationType_fromClientValue() {
        assertEquals(ScreenOrientationType.PORTRAIT_PRIMARY,
                ScreenOrientationType.fromClientValue("portrait-primary"));
        assertEquals(ScreenOrientationType.LANDSCAPE_SECONDARY,
                ScreenOrientationType.fromClientValue("landscape-secondary"));
        assertEquals(ScreenOrientationType.UNSUPPORTED,
                ScreenOrientationType.fromClientValue("unsupported"));
        assertThrows(IllegalArgumentException.class,
                () -> ScreenOrientationType.fromClientValue("diagonal-future"));
    }

    @Test
    void isLandscape_isPortrait() {
        assertTrue(ScreenOrientationType.LANDSCAPE_PRIMARY.isLandscape());
        assertTrue(ScreenOrientationType.LANDSCAPE_SECONDARY.isLandscape());
        assertFalse(ScreenOrientationType.PORTRAIT_PRIMARY.isLandscape());
        assertFalse(ScreenOrientationType.UNKNOWN.isLandscape());
        assertFalse(ScreenOrientationType.UNSUPPORTED.isLandscape());

        assertTrue(ScreenOrientationType.PORTRAIT_PRIMARY.isPortrait());
        assertTrue(ScreenOrientationType.PORTRAIT_SECONDARY.isPortrait());
        assertFalse(ScreenOrientationType.LANDSCAPE_PRIMARY.isPortrait());
        assertFalse(ScreenOrientationType.UNKNOWN.isPortrait());
        assertFalse(ScreenOrientationType.UNSUPPORTED.isPortrait());
    }

    @Test
    void setStateFromClient_unsupportedFromBootstrap() {
        ui.getInternals().setScreenOrientationFromClient("unsupported", "0");
        assertEquals(ScreenOrientationType.UNSUPPORTED,
                ScreenOrientation.orientationSignal().peek().type(),
                "Client-side 'unsupported' must be observable distinctly "
                        + "from the pre-bootstrap UNKNOWN state");
    }

    @Test
    void isScreenOrientationSupported_reflectsSignalState() {
        ExtendedClientDetails details = ui.getPage().getExtendedClientDetails();

        assertFalse(details.isScreenOrientationSupported(),
                "Before bootstrap (UNKNOWN) the feature-detect must be "
                        + "false so callers don't expose unusable UI");

        ui.getInternals().setScreenOrientationFromClient("unsupported", "0");
        assertFalse(details.isScreenOrientationSupported(),
                "UNSUPPORTED bootstrap value must yield false");

        ui.getInternals().setScreenOrientationFromClient("landscape-primary",
                "90");
        assertTrue(details.isScreenOrientationSupported(),
                "A real orientation from the bootstrap means the API is "
                        + "available");
    }

    private static void resolveLockPromise(MockUI ui, ObjectNode result) {
        PendingJavaScriptInvocation invocation = ui.dumpPendingJsInvocations()
                .stream()
                .filter(inv -> inv.getInvocation().getExpression()
                        .contains("screenOrientation.lock"))
                .reduce((a, b) -> b).orElseThrow();
        invocation.complete(result);
    }

    private void fireOrientationEvent(UI ui, String type, int angle) {
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
