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
package com.vaadin.flow.component.geolocation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeolocationTest {

    private MockUI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }

    // --- Record Jackson round-trip tests ---

    @Test
    void geolocationCoordinates_jacksonRoundTrip() {
        GeolocationCoordinates coords = new GeolocationCoordinates(60.1699,
                24.9384, 10.0, 25.5, 5.0, 90.0, 1.5);

        ObjectNode json = JacksonUtils.beanToJson(coords);
        GeolocationCoordinates result = JacksonUtils.readValue(json,
                GeolocationCoordinates.class);

        assertEquals(coords.latitude(), result.latitude());
        assertEquals(coords.longitude(), result.longitude());
        assertEquals(coords.accuracy(), result.accuracy());
        assertEquals(coords.altitude(), result.altitude());
        assertEquals(coords.altitudeAccuracy(), result.altitudeAccuracy());
        assertEquals(coords.heading(), result.heading());
        assertEquals(coords.speed(), result.speed());
    }

    @Test
    void geolocationPosition_jacksonRoundTrip() {
        GeolocationCoordinates coords = new GeolocationCoordinates(60.1699,
                24.9384, 10.0, null, null, null, null);
        GeolocationPosition pos = new GeolocationPosition(coords,
                1700000000000L);

        ObjectNode json = JacksonUtils.beanToJson(pos);
        GeolocationPosition result = JacksonUtils.readValue(json,
                GeolocationPosition.class);

        assertEquals(pos.timestamp(), result.timestamp());
        assertEquals(pos.coords().latitude(), result.coords().latitude());
        assertEquals(pos.coords().longitude(), result.coords().longitude());
    }

    @Test
    void geolocationPosition_timestampAsInstantReturnsInstant() {
        GeolocationPosition pos = new GeolocationPosition(
                new GeolocationCoordinates(60.1699, 24.9384, 10.0, null, null,
                        null, null),
                1700000000000L);

        assertEquals(Instant.ofEpochMilli(1700000000000L),
                pos.timestampAsInstant());
    }

    @Test
    void geolocationError_jacksonRoundTrip() {
        GeolocationError error = new GeolocationError(
                GeolocationErrorCode.PERMISSION_DENIED.code(),
                "User denied geolocation");

        ObjectNode json = JacksonUtils.beanToJson(error);
        GeolocationError result = JacksonUtils.readValue(json,
                GeolocationError.class);

        assertEquals(error.code(), result.code());
        assertEquals(error.message(), result.message());
    }

    @Test
    void geolocationError_errorCode_mapsKnownCodes() {
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED,
                new GeolocationError(1, "denied").errorCode());
        assertEquals(GeolocationErrorCode.POSITION_UNAVAILABLE,
                new GeolocationError(2, "unavailable").errorCode());
        assertEquals(GeolocationErrorCode.TIMEOUT,
                new GeolocationError(3, "timeout").errorCode());
    }

    @Test
    void geolocationError_errorCode_returnsUnknownForUnrecognisedCode() {
        assertEquals(GeolocationErrorCode.UNKNOWN,
                new GeolocationError(99, "future code").errorCode());
    }

    // --- UI.getGeolocation() tests ---

    @Test
    void getGeolocation_returnsSameInstanceOnRepeatedCalls() {
        assertSame(ui.getGeolocation(), ui.getGeolocation());
    }

    @Test
    void newGeolocation_throwsWhenAlreadyCreated() {
        // UI.getGeolocation() is populated by the UI constructor, so a
        // second direct construction must be rejected.
        assertThrows(IllegalStateException.class, () -> new Geolocation(ui));
    }

    // --- getPosition() tests ---

    @Test
    void getPosition_executesPromiseJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().getPosition(pos -> {
        }, err -> {
        });

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.get")));
    }

    @Test
    void getPosition_onSuccessReceivesPositionAndOnErrorIsSilent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        ui.getGeolocation().getPosition(positions::add, errors::add);

        resolvePromise(ui,
                resultJson(position(60.1699, 24.9384, 10.0), null, "GRANTED"));

        assertEquals(1, positions.size());
        assertEquals(60.1699, positions.get(0).coords().latitude());
        assertTrue(errors.isEmpty(),
                "onError must not fire for a successful reading");
    }

    @Test
    void getPosition_onErrorReceivesErrorAndOnSuccessIsSilent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        ui.getGeolocation().getPosition(positions::add, errors::add);

        resolvePromise(ui, resultJson(null, error(1, "denied"), "DENIED"));

        assertEquals(1, errors.size());
        assertEquals(1, errors.get(0).code());
        assertTrue(positions.isEmpty(),
                "onSuccess must not fire when the browser reports an error");
    }

    @Test
    void getPosition_updatesAvailabilityFromResponse() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().getPosition(pos -> {
        }, err -> {
        });
        resolvePromise(ui,
                resultJson(position(60.0, 25.0, 10.0), null, "GRANTED"));

        assertEquals(GeolocationAvailability.GRANTED,
                ui.getInternals().getGeolocationAvailabilitySignal().peek());
    }

    // --- watchPosition() tests ---

    @Test
    void watchPosition_registersListenersAndExecutesWatchJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        assertNotNull(watcher);
        assertNotNull(watcher.valueSignal());
        assertInstanceOf(GeolocationPending.class,
                watcher.valueSignal().peek());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.watch")));
    }

    @Test
    void watchPosition_signalSurfacesUnknownErrorWhenWatchExecuteJsFails() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        PendingJavaScriptInvocation watchInvocation = ui
                .dumpPendingJsInvocations().stream()
                .filter(inv -> inv.getInvocation().getExpression()
                        .contains("geolocation.watch"))
                .reduce((a, b) -> b).orElseThrow();
        watchInvocation.completeExceptionally(
                JacksonUtils.createNode("module not loaded"));

        GeolocationResult value = watcher.valueSignal().peek();
        GeolocationError err = assertInstanceOf(GeolocationError.class, value,
                "watch executeJs failure must surface as a GeolocationError"
                        + " on the watcher's valueSignal");
        assertEquals(GeolocationErrorCode.UNKNOWN, err.errorCode());
        assertFalse(err.message().contains("module not loaded"),
                "synthesized message must not leak the wrapped client text;"
                        + " the cause is logged at DEBUG instead");
        assertTrue(watcher.activeSignal().peek(),
                "activeSignal stays true on infra failure — its contract"
                        + " is tied to resume()/stop(), not data flow");
    }

    @Test
    void watchPosition_signalUpdatesOnPositionEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", 60.1699);
        coords.put("longitude", 24.9384);
        coords.put("accuracy", 10.0);
        coords.put("altitude", 25.5);
        coords.put("altitudeAccuracy", 5.0);
        coords.put("heading", 90.0);
        coords.put("speed", 1.5);
        detail.set("coords", coords);
        detail.put("timestamp", 1700000000000L);
        eventData.set("event.detail", detail);

        fireEvent(component.getElement(), "vaadin-geolocation-position",
                eventData);

        assertInstanceOf(GeolocationPosition.class,
                watcher.valueSignal().peek());
        GeolocationPosition pos = (GeolocationPosition) watcher.valueSignal()
                .peek();
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(24.9384, pos.coords().longitude());
        assertEquals(10.0, pos.coords().accuracy());
        assertEquals(25.5, pos.coords().altitude());
        assertEquals(5.0, pos.coords().altitudeAccuracy());
        assertEquals(90.0, pos.coords().heading());
        assertEquals(1.5, pos.coords().speed());
        assertEquals(1700000000000L, pos.timestamp());
    }

    @Test
    void watchPosition_signalUpdatesOnErrorEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("code", GeolocationErrorCode.PERMISSION_DENIED.code());
        detail.put("message", "User denied geolocation");
        eventData.set("event.detail", detail);

        fireEvent(component.getElement(), "vaadin-geolocation-error",
                eventData);

        assertInstanceOf(GeolocationError.class, watcher.valueSignal().peek());
        GeolocationError error = (GeolocationError) watcher.valueSignal()
                .peek();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                error.code());
        assertEquals("User denied geolocation", error.message());
    }

    @Test
    void watchPosition_stateTransitionsFromErrorToPosition() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        ObjectNode errEventData = JacksonUtils.createObjectNode();
        ObjectNode errDetail = JacksonUtils.createObjectNode();
        errDetail.put("code", GeolocationErrorCode.TIMEOUT.code());
        errDetail.put("message", "Timeout");
        errEventData.set("event.detail", errDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-error",
                errEventData);
        assertInstanceOf(GeolocationError.class, watcher.valueSignal().peek());

        ObjectNode posEventData = JacksonUtils.createObjectNode();
        ObjectNode posDetail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", 60.1699);
        coords.put("longitude", 24.9384);
        coords.put("accuracy", 10.0);
        posDetail.set("coords", coords);
        posDetail.put("timestamp", 1700000000000L);
        posEventData.set("event.detail", posDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-position",
                posEventData);

        assertInstanceOf(GeolocationPosition.class,
                watcher.valueSignal().peek());
    }

    @Test
    void watchPosition_autoStopsOnDetach() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().watchPosition(component);

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-error")
                .isEmpty());

        ui.dumpPendingJsInvocations();
        ui.remove(component);

        assertTrue(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());
        assertTrue(listenerMap.getExpressions("vaadin-geolocation-error")
                .isEmpty());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_removesListenersAndQueuesClearWatch() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());

        ui.dumpPendingJsInvocations();
        watcher.stop();

        assertTrue(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());
        assertTrue(listenerMap.getExpressions("vaadin-geolocation-error")
                .isEmpty());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_isIdempotent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        ui.dumpPendingJsInvocations();

        watcher.stop();
        ui.dumpPendingJsInvocations();

        assertDoesNotThrow(watcher::stop);
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().noneMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_afterDetach_isNoOp() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        ui.remove(component);
        ui.dumpPendingJsInvocations();

        assertDoesNotThrow(watcher::stop);
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().noneMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void resume_restartsAfterStop() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);

        watcher.stop();
        assertTrue(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());

        ui.dumpPendingJsInvocations();
        watcher.resume();

        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());
        assertInstanceOf(GeolocationPending.class,
                watcher.valueSignal().peek());
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.watch")));
    }

    @Test
    void resume_isNoOpWhenActive() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        ui.dumpPendingJsInvocations();

        watcher.resume();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().noneMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.watch")));
    }

    @Test
    void active_signalReflectsResumeAndStop() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        assertTrue(watcher.activeSignal().peek());

        watcher.stop();
        assertFalse(watcher.activeSignal().peek());

        watcher.resume();
        assertTrue(watcher.activeSignal().peek());
    }

    // --- addPositionListener() tests ---

    @Test
    void addPositionListener_dispatchesPositionAndError() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        watcher.addPositionListener(positions::add, errors::add);

        ObjectNode posData = JacksonUtils.createObjectNode();
        ObjectNode posDetail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", 60.1699);
        coords.put("longitude", 24.9384);
        coords.put("accuracy", 10.0);
        posDetail.set("coords", coords);
        posDetail.put("timestamp", 1700000000000L);
        posData.set("event.detail", posDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-position",
                posData);

        ObjectNode errData = JacksonUtils.createObjectNode();
        ObjectNode errDetail = JacksonUtils.createObjectNode();
        errDetail.put("code", GeolocationErrorCode.TIMEOUT.code());
        errDetail.put("message", "Timeout");
        errData.set("event.detail", errDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-error", errData);

        assertEquals(1, positions.size());
        assertEquals(60.1699, positions.get(0).coords().latitude());
        assertEquals(1, errors.size());
        assertEquals(GeolocationErrorCode.TIMEOUT, errors.get(0).errorCode());
    }

    @Test
    void addPositionListener_registrationStopsDelivery() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        Registration registration = watcher.addPositionListener(positions::add,
                err -> {
                });

        registration.remove();

        ObjectNode posData = JacksonUtils.createObjectNode();
        ObjectNode posDetail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", 60.0);
        coords.put("longitude", 25.0);
        coords.put("accuracy", 10.0);
        posDetail.set("coords", coords);
        posDetail.put("timestamp", 1700000000000L);
        posData.set("event.detail", posDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-position",
                posData);

        assertTrue(positions.isEmpty(),
                "removed listener must not receive subsequent updates");
    }

    @Test
    void addPositionListener_survivesStopResume() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        watcher.addPositionListener(positions::add, err -> {
        });

        watcher.stop();
        watcher.resume();

        ObjectNode posData = JacksonUtils.createObjectNode();
        ObjectNode posDetail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", 60.0);
        coords.put("longitude", 25.0);
        coords.put("accuracy", 10.0);
        posDetail.set("coords", coords);
        posDetail.put("timestamp", 1700000000000L);
        posData.set("event.detail", posDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-position",
                posData);

        assertEquals(1, positions.size());
        assertEquals(60.0, positions.get(0).coords().latitude());
    }

    @Test
    void addPositionListener_pendingIsSilent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = ui.getGeolocation()
                .watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        watcher.addPositionListener(positions::add, errors::add);

        // resume() resets the signal to Pending — listeners must stay silent
        // since they only see real outcomes.
        watcher.stop();
        watcher.resume();

        assertTrue(positions.isEmpty());
        assertTrue(errors.isEmpty());
    }

    // --- availability() / availability-change listener tests ---

    @Test
    void availability_unknownBeforeAnyReport() {
        assertEquals(GeolocationAvailability.UNKNOWN,
                ui.getGeolocation().availabilitySignal().peek());
    }

    @Test
    void availability_reflectsUIInternalsSignal() {
        ui.getInternals()
                .setGeolocationAvailability(GeolocationAvailability.GRANTED);

        assertEquals(GeolocationAvailability.GRANTED,
                ui.getGeolocation().availabilitySignal().peek());
    }

    @Test
    void availabilityChangeListener_isRegisteredFromConstructor() {
        ElementListenerMap listenerMap = ui.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap
                .getExpressions("vaadin-geolocation-availability-change")
                .isEmpty());
    }

    @Test
    void availabilityChangeListener_updatesCachedValue() {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("availability", "DENIED");
        eventData.set("event.detail", detail);

        fireEvent(ui.getElement(), "vaadin-geolocation-availability-change",
                eventData);

        assertEquals(GeolocationAvailability.DENIED,
                ui.getInternals().getGeolocationAvailabilitySignal().peek());
    }

    @Test
    void availabilityChangeListener_ignoresUnknownValue() {
        ui.getInternals()
                .setGeolocationAvailability(GeolocationAvailability.GRANTED);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("availability", "BOGUS");
        eventData.set("event.detail", detail);
        fireEvent(ui.getElement(), "vaadin-geolocation-availability-change",
                eventData);

        assertEquals(GeolocationAvailability.GRANTED,
                ui.getInternals().getGeolocationAvailabilitySignal().peek());
    }

    // --- GeolocationOptions tests ---

    @Test
    void geolocationOptions_builder_convertsDurationsToMillis() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(10))
                .maximumAge(Duration.ZERO).build();

        assertEquals(Boolean.TRUE, opts.enableHighAccuracy());
        assertEquals(10_000, opts.timeout());
        assertEquals(0, opts.maximumAge());
    }

    @Test
    void geolocationOptions_builder_intMillisOverloads() {
        GeolocationOptions opts = GeolocationOptions.builder().timeout(5_000)
                .maximumAge(1_000).build();

        assertEquals(5_000, opts.timeout());
        assertEquals(1_000, opts.maximumAge());
    }

    @Test
    void geolocationOptions_builder_leavesUnsetFieldsNull() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .timeout(Duration.ofSeconds(5)).build();

        assertNull(opts.enableHighAccuracy());
        assertEquals(5_000, opts.timeout());
        assertNull(opts.maximumAge());
    }

    @Test
    void geolocationOptions_rejectsNegativeTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeolocationOptions((Boolean) null, -1,
                        (Integer) null));
        GeolocationOptions.Builder builder = GeolocationOptions.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.timeout(-1));
    }

    @Test
    void geolocationOptions_rejectsNegativeMaximumAge() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeolocationOptions((Boolean) null, (Integer) null,
                        -1));
        GeolocationOptions.Builder builder = GeolocationOptions.builder();
        assertThrows(IllegalArgumentException.class,
                () -> builder.maximumAge(-1));
    }

    // --- Helpers ---

    private static void fireEvent(Element element, String eventType,
            ObjectNode eventData) {
        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);
        listenerMap.fireEvent(new DomEvent(element, eventType, eventData));
    }

    /**
     * Resolves the most recent pending geolocation.get() executeJs call by
     * delivering {@code resultJson} to its {@code then(Class, ...)} callback,
     * mimicking what the client would send back.
     */
    private static void resolvePromise(MockUI ui, ObjectNode resultJson) {
        PendingJavaScriptInvocation invocation = ui.dumpPendingJsInvocations()
                .stream()
                .filter(inv -> inv.getInvocation().getExpression()
                        .contains("geolocation.get"))
                .reduce((a, b) -> b).orElseThrow();
        invocation.complete(resultJson);
    }

    private static ObjectNode resultJson(@Nullable ObjectNode position,
            @Nullable ObjectNode error, @Nullable String availability) {
        ObjectNode result = JacksonUtils.createObjectNode();
        if (position != null) {
            result.set("position", position);
        }
        if (error != null) {
            result.set("error", error);
        }
        if (availability != null) {
            result.put("availability", availability);
        }
        return result;
    }

    private static ObjectNode position(double lat, double lon,
            double accuracy) {
        ObjectNode position = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", lat);
        coords.put("longitude", lon);
        coords.put("accuracy", accuracy);
        position.set("coords", coords);
        position.put("timestamp", 1700000000000L);
        return position;
    }

    private static ObjectNode error(int code, String message) {
        ObjectNode error = JacksonUtils.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        return error;
    }
}
