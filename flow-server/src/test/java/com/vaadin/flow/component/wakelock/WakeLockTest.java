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
package com.vaadin.flow.component.wakelock;

import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WakeLockTest {

    @Test
    void activeSignal_isReadOnly() {
        MockUI ui = new MockUI();
        Signal<Boolean> signal = WakeLock.activeSignal(ui);
        assertFalse(signal instanceof ValueSignal,
                "activeSignal() should return a read-only signal");
    }

    @Test
    void activeSignal_defaultsToFalse() {
        MockUI ui = new MockUI();
        assertEquals(Boolean.FALSE, WakeLock.activeSignal(ui).peek(),
                "Before any client confirmation the lock is not held");
    }

    @Test
    void activeSignal_readonlyWrapperIsCached() {
        MockUI ui = new MockUI();
        assertSame(WakeLock.activeSignal(ui), WakeLock.activeSignal(ui),
                "Repeated calls must return the same read-only wrapper so "
                        + "subscriber identity stays stable");
    }

    @Test
    void activeSignal_tracksStateChanges() {
        MockUI ui = new MockUI();
        Signal<Boolean> active = WakeLock.activeSignal(ui);

        fireStateEvent(ui, "ACTIVE");
        assertEquals(Boolean.TRUE, active.peek());

        fireStateEvent(ui, "RELEASED");
        assertEquals(Boolean.FALSE, active.peek());
    }

    @Test
    void activeSignal_unknownDetailKeepsPreviousValue() {
        MockUI ui = new MockUI();
        Signal<Boolean> active = WakeLock.activeSignal(ui);

        fireStateEvent(ui, "ACTIVE");
        fireStateEvent(ui, "SOMETHING_NEW");

        assertEquals(Boolean.TRUE, active.peek(),
                "Unknown detail values from a newer client should not reset "
                        + "the signal");
    }

    @Test
    void request_executesClientCall() {
        MockUI ui = new MockUI();
        WakeLock.request(ui);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.request(this)")),
                "request() should invoke window.Vaadin.Flow.wakeLock.request");
    }

    @Test
    void release_executesClientCall() {
        MockUI ui = new MockUI();
        WakeLock.release(ui);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.release(this)")),
                "release() should invoke window.Vaadin.Flow.wakeLock.release");
    }

    @Test
    void availabilitySignal_defaultsToUnknown() {
        MockUI ui = new MockUI();
        assertEquals(WakeLockAvailability.UNKNOWN,
                WakeLock.availabilitySignal(ui).peek(),
                "Before bootstrap reports a value the availability is UNKNOWN");
    }

    @Test
    void availabilitySignal_readonlyWrapperIsCached() {
        MockUI ui = new MockUI();
        assertSame(WakeLock.availabilitySignal(ui),
                WakeLock.availabilitySignal(ui),
                "Repeated calls must return the same read-only wrapper");
    }

    @Test
    void availabilitySignal_tracksUIInternals() {
        MockUI ui = new MockUI();
        Signal<WakeLockAvailability> availability = WakeLock
                .availabilitySignal(ui);

        ui.getInternals()
                .setWakeLockAvailability(WakeLockAvailability.SUPPORTED);
        assertEquals(WakeLockAvailability.SUPPORTED, availability.peek());

        ui.getInternals()
                .setWakeLockAvailability(WakeLockAvailability.UNSUPPORTED);
        assertEquals(WakeLockAvailability.UNSUPPORTED, availability.peek());
    }

    @Test
    void request_withErrorHandler_failsFastWhenUnsupported() {
        MockUI ui = new MockUI();
        ui.getInternals()
                .setWakeLockAvailability(WakeLockAvailability.UNSUPPORTED);
        AtomicReference<WakeLockError> received = new AtomicReference<>();

        WakeLock.request(received::set, ui);

        WakeLockError error = received.get();
        assertNotNull(error, "Error handler should fire on UNSUPPORTED before "
                + "the client round-trip");
        assertEquals(WakeLockErrorCode.UNSUPPORTED, error.code());
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .noneMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.request")),
                "Fail-fast on UNSUPPORTED should not invoke the client");
    }

    @Test
    void request_withErrorHandler_invokesClientWhenSupported() {
        MockUI ui = new MockUI();
        ui.getInternals()
                .setWakeLockAvailability(WakeLockAvailability.SUPPORTED);
        AtomicReference<WakeLockError> received = new AtomicReference<>();

        WakeLock.request(received::set, ui);

        assertNull(received.get(), "No error before the client reports back");
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "return window.Vaadin.Flow.wakeLock.request(this)")),
                "Request should round-trip via executeJs with a return value");
    }

    @Test
    void request_withErrorHandler_invokesClientWhenAvailabilityUnknown() {
        MockUI ui = new MockUI();
        AtomicReference<WakeLockError> received = new AtomicReference<>();

        WakeLock.request(received::set, ui);

        assertNull(received.get(),
                "UNKNOWN availability must not preempt the client call");
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression().contains(
                        "return window.Vaadin.Flow.wakeLock.request(this)")));
    }

    private void fireStateEvent(MockUI ui, String state) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail", state);
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        WakeLock.STATE_CHANGE_EVENT, eventData));
    }
}
