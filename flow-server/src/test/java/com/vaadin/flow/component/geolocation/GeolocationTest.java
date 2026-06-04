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
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(error.debugInfo(), result.debugInfo());
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

    // --- getPosition() tests ---

    @Test
    void getPosition_executesPromiseJs() {
        Geolocation.getPosition(pos -> {
        }, err -> {
        }, ui);

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.get")));
    }

    @Test
    void getPosition_callbackReceivesPosition() {
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        Geolocation.getPosition(positions::add, errors::add, ui);

        resolvePromise(ui,
                resultJson(position(60.1699, 24.9384, 10.0), null, "GRANTED"));

        assertEquals(1, positions.size());
        assertTrue(errors.isEmpty());
        assertEquals(60.1699, positions.get(0).coords().latitude());
    }

    @Test
    void getPosition_callbackReceivesError() {
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        Geolocation.getPosition(positions::add, errors::add, ui);

        resolvePromise(ui, resultJson(null, error(1, "denied"), "DENIED"));

        assertEquals(1, errors.size());
        assertTrue(positions.isEmpty());
        assertEquals(1, errors.get(0).code());
    }

    @Test
    void getPosition_callbackException_routesToErrorHandler() {
        List<Throwable> caught = new ArrayList<>();
        ErrorHandler handler = event -> caught.add(event.getThrowable());
        Mockito.when(ui.getSession().getErrorHandler()).thenReturn(handler);

        Geolocation.getPosition(pos -> {
            throw new RuntimeException("boom");
        }, err -> {
        }, ui);

        resolvePromise(ui,
                resultJson(position(60.0, 25.0, 10.0), null, "GRANTED"));

        assertEquals(1, caught.size());
        assertEquals("boom", caught.get(0).getMessage());
    }

    @Test
    void getPosition_updatesAvailabilityFromResponse() {
        Geolocation.getPosition(pos -> {
        }, err -> {
        }, ui);

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

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        assertNotNull(watcher);
        assertNotNull(watcher.positionSignal());
        assertInstanceOf(GeolocationPending.class,
                watcher.positionSignal().peek());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.watch")));
    }

    @Test
    void watchPosition_unattachedOwner_activatesOnFirstAttach() {
        TestComponent component = new TestComponent();

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        assertNull(watcher.handle(),
                "watch must not start before owner is attached");
        assertFalse(watcher.activeSignal().peek(),
                "watcher must not be active before owner is attached");

        ui.add(component);

        assertNotNull(watcher.handle(),
                "watch should start when owner attaches");
        assertTrue(watcher.activeSignal().peek(),
                "watcher should be active after owner attaches");
    }

    @Test
    void watchPosition_signalUpdatesOnPositionEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        firePosition(component, 60.1699, 24.9384);

        assertInstanceOf(GeolocationPosition.class,
                watcher.positionSignal().peek());
        GeolocationPosition pos = (GeolocationPosition) watcher.positionSignal()
                .peek();
        assertEquals(60.1699, pos.coords().latitude());
        assertEquals(24.9384, pos.coords().longitude());
    }

    @Test
    void watchPosition_signalUpdatesOnErrorEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        fireError(component, GeolocationErrorCode.PERMISSION_DENIED.code(),
                "User denied geolocation");

        assertInstanceOf(GeolocationError.class,
                watcher.positionSignal().peek());
        GeolocationError error = (GeolocationError) watcher.positionSignal()
                .peek();
        assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                error.code());
        assertEquals("User denied geolocation", error.debugInfo());
    }

    @Test
    void watchPosition_autoStopsOnDetach() {
        TestComponent component = new TestComponent();
        ui.add(component);

        Geolocation.watchPosition(component);

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());

        ui.dumpPendingJsInvocations();
        ui.remove(component);

        assertTrue(listenerMap.getExpressions("vaadin-geolocation-position")
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

        GeolocationWatcher watcher = Geolocation.watchPosition(component);

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());

        ui.dumpPendingJsInvocations();
        watcher.stop();

        assertTrue(listenerMap.getExpressions("vaadin-geolocation-position")
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

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
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
    void resume_restartsAfterStop() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        watcher.stop();
        ui.dumpPendingJsInvocations();

        watcher.resume();

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        assertFalse(listenerMap.getExpressions("vaadin-geolocation-position")
                .isEmpty());
        assertInstanceOf(GeolocationPending.class,
                watcher.positionSignal().peek());
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream().anyMatch(inv -> inv.getInvocation()
                .getExpression().contains("geolocation.watch")));
    }

    @Test
    void active_signalReflectsResumeAndStop() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        assertTrue(watcher.activeSignal().peek());

        watcher.stop();
        assertFalse(watcher.activeSignal().peek());

        watcher.resume();
        assertTrue(watcher.activeSignal().peek());
    }

    // --- addPositionListener() tests ---

    @Test
    void addPositionListener_firesSuccessOnPositionEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        watcher.addPositionListener(positions::add, errors::add);

        firePosition(component, 60.1699, 24.9384);

        assertEquals(1, positions.size());
        assertEquals(60.1699, positions.get(0).coords().latitude());
        assertTrue(errors.isEmpty());
    }

    @Test
    void addPositionListener_firesErrorOnErrorEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        watcher.addPositionListener(positions::add, errors::add);

        fireError(component, GeolocationErrorCode.TIMEOUT.code(), "Timeout");

        assertTrue(positions.isEmpty());
        assertEquals(1, errors.size());
        assertEquals(GeolocationErrorCode.TIMEOUT.code(), errors.get(0).code());
    }

    @Test
    void addPositionListener_listenerException_routesToErrorHandlerAndContinues() {
        TestComponent component = new TestComponent();
        ui.add(component);
        List<Throwable> caught = new ArrayList<>();
        ErrorHandler handler = event -> caught.add(event.getThrowable());
        Mockito.when(ui.getSession().getErrorHandler()).thenReturn(handler);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> later = new ArrayList<>();
        watcher.addPositionListener(pos -> {
            throw new RuntimeException("boom");
        }, err -> {
        });
        watcher.addPositionListener(later::add, err -> {
        });

        firePosition(component, 60.1699, 24.9384);

        assertEquals(1, caught.size());
        assertEquals("boom", caught.get(0).getMessage());
        assertEquals(1, later.size(),
                "later listener must still receive the reading after an earlier listener throws");
    }

    @Test
    void addPositionListener_doesNotFireOnPendingState() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        watcher.addPositionListener(positions::add, errors::add);

        // resume() resets to Pending; listeners must not fire.
        watcher.stop();
        watcher.resume();

        assertTrue(positions.isEmpty());
        assertTrue(errors.isEmpty());
    }

    @Test
    void addPositionListener_registrationRemoveStopsBothConsumers() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        List<GeolocationError> errors = new ArrayList<>();
        Registration reg = watcher.addPositionListener(positions::add,
                errors::add);

        reg.remove();

        firePosition(component, 60.0, 25.0);
        fireError(component, 1, "denied");

        assertTrue(positions.isEmpty());
        assertTrue(errors.isEmpty());
    }

    @Test
    void addPositionListener_survivesStopAndResume() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        watcher.addPositionListener(positions::add, err -> {
        });

        watcher.stop();
        watcher.resume();
        firePosition(component, 60.0, 25.0);

        assertEquals(1, positions.size());
    }

    @Test
    void addPositionListener_signalAndListenersBothFire() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationWatcher watcher = Geolocation.watchPosition(component);
        List<GeolocationPosition> positions = new ArrayList<>();
        watcher.addPositionListener(positions::add, err -> {
        });

        firePosition(component, 60.0, 25.0);

        assertEquals(1, positions.size());
        assertInstanceOf(GeolocationPosition.class,
                watcher.positionSignal().peek());
    }

    // --- availabilityHintSignal() tests ---

    @Test
    void availability_unknownBeforeAnyReport() {
        assertEquals(GeolocationAvailability.UNKNOWN,
                Geolocation.availabilityHintSignal(ui).peek());
    }

    @Test
    void availability_reflectsUIInternalsSignal() {
        // Resolve the signal first so the client is installed.
        Geolocation.availabilityHintSignal(ui);
        ui.getInternals()
                .setGeolocationAvailability(GeolocationAvailability.GRANTED);

        assertEquals(GeolocationAvailability.GRANTED,
                Geolocation.availabilityHintSignal(ui).peek());
    }

    @Test
    void availabilityHintSignal_installsClientLazily() {
        assertNull(ui.getInternals().getGeolocationClient());

        Geolocation.availabilityHintSignal(ui);

        assertNotNull(ui.getInternals().getGeolocationClient());
    }

    @Test
    void availabilityChangeListener_updatesCachedValue() {
        // Trigger lazy installation so the availability-change listener is
        // registered on the UI element.
        Geolocation.availabilityHintSignal(ui);

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
        Geolocation.availabilityHintSignal(ui);
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

    private static void firePosition(Component component, double lat,
            double lon) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        ObjectNode coords = JacksonUtils.createObjectNode();
        coords.put("latitude", lat);
        coords.put("longitude", lon);
        coords.put("accuracy", 10.0);
        detail.set("coords", coords);
        detail.put("timestamp", 1700000000000L);
        eventData.set("event.detail", detail);
        fireEvent(component.getElement(), "vaadin-geolocation-position",
                eventData);
    }

    private static void fireError(Component component, int code,
            String message) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("code", code);
        detail.put("message", message);
        eventData.set("event.detail", detail);
        fireEvent(component.getElement(), "vaadin-geolocation-error",
                eventData);
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
