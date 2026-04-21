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

import java.util.List;

import org.junit.jupiter.api.Assertions;
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
import com.vaadin.tests.util.MockUI;

public class GeolocationTest {

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

        Assertions.assertEquals(coords.latitude(), result.latitude());
        Assertions.assertEquals(coords.longitude(), result.longitude());
        Assertions.assertEquals(coords.accuracy(), result.accuracy());
        Assertions.assertEquals(coords.altitude(), result.altitude());
        Assertions.assertEquals(coords.altitudeAccuracy(),
                result.altitudeAccuracy());
        Assertions.assertEquals(coords.heading(), result.heading());
        Assertions.assertEquals(coords.speed(), result.speed());
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

        Assertions.assertEquals(pos.timestamp(), result.timestamp());
        Assertions.assertEquals(pos.coords().latitude(),
                result.coords().latitude());
        Assertions.assertEquals(pos.coords().longitude(),
                result.coords().longitude());
    }

    @Test
    void geolocationError_jacksonRoundTrip() {
        GeolocationError error = new GeolocationError(
                GeolocationErrorCode.PERMISSION_DENIED.code(),
                "User denied geolocation");

        ObjectNode json = JacksonUtils.beanToJson(error);
        GeolocationError result = JacksonUtils.readValue(json,
                GeolocationError.class);

        Assertions.assertEquals(error.code(), result.code());
        Assertions.assertEquals(error.message(), result.message());
    }

    @Test
    void geolocationError_errorCode_mapsKnownCodes() {
        Assertions.assertEquals(GeolocationErrorCode.PERMISSION_DENIED,
                new GeolocationError(1, "denied").errorCode());
        Assertions.assertEquals(GeolocationErrorCode.POSITION_UNAVAILABLE,
                new GeolocationError(2, "unavailable").errorCode());
        Assertions.assertEquals(GeolocationErrorCode.TIMEOUT,
                new GeolocationError(3, "timeout").errorCode());
    }

    @Test
    void geolocationError_errorCode_returnsNullForUnknownCode() {
        Assertions.assertNull(
                new GeolocationError(99, "future code").errorCode());
    }

    // --- UI.getGeolocation() tests ---

    @Test
    void getGeolocation_returnsSameInstanceOnRepeatedCalls() {
        Assertions.assertSame(ui.getGeolocation(), ui.getGeolocation());
    }

    // --- get() tests ---

    @Test
    void get_executesPromiseJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().get(result -> {
        });

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(invocations.stream().anyMatch(inv -> inv
                .getInvocation().getExpression().contains("geolocation.get")));
    }

    // --- track() tests ---

    @Test
    void track_registersListenersAndExecutesWatchJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);

        Assertions.assertNotNull(tracker);
        Assertions.assertNotNull(tracker.value());
        Assertions.assertInstanceOf(GeolocationResult.Pending.class,
                tracker.value().peek());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().anyMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.watch")));
    }

    @Test
    void track_signalUpdatesOnPositionEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);

        // Simulate a position event with all coordinate fields
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

        Assertions.assertInstanceOf(GeolocationPosition.class,
                tracker.value().peek());
        GeolocationPosition pos = (GeolocationPosition) tracker.value().peek();
        Assertions.assertEquals(60.1699, pos.coords().latitude());
        Assertions.assertEquals(24.9384, pos.coords().longitude());
        Assertions.assertEquals(10.0, pos.coords().accuracy());
        Assertions.assertEquals(25.5, pos.coords().altitude());
        Assertions.assertEquals(5.0, pos.coords().altitudeAccuracy());
        Assertions.assertEquals(90.0, pos.coords().heading());
        Assertions.assertEquals(1.5, pos.coords().speed());
        Assertions.assertEquals(1700000000000L, pos.timestamp());
    }

    @Test
    void track_signalUpdatesOnErrorEvent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);

        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("code", GeolocationErrorCode.PERMISSION_DENIED.code());
        detail.put("message", "User denied geolocation");
        eventData.set("event.detail", detail);

        fireEvent(component.getElement(), "vaadin-geolocation-error",
                eventData);

        Assertions.assertInstanceOf(GeolocationError.class,
                tracker.value().peek());
        GeolocationError error = (GeolocationError) tracker.value().peek();
        Assertions.assertEquals(GeolocationErrorCode.PERMISSION_DENIED.code(),
                error.code());
        Assertions.assertEquals("User denied geolocation", error.message());
    }

    @Test
    void track_stateTransitionsFromErrorToPosition() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);

        // First simulate an error
        ObjectNode errEventData = JacksonUtils.createObjectNode();
        ObjectNode errDetail = JacksonUtils.createObjectNode();
        errDetail.put("code", GeolocationErrorCode.TIMEOUT.code());
        errDetail.put("message", "Timeout");
        errEventData.set("event.detail", errDetail);
        fireEvent(component.getElement(), "vaadin-geolocation-error",
                errEventData);

        Assertions.assertInstanceOf(GeolocationError.class,
                tracker.value().peek());

        // Then simulate a successful position
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

        Assertions.assertInstanceOf(GeolocationPosition.class,
                tracker.value().peek());
    }

    @Test
    void track_autoStopsOnDetach() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().track(component);

        // Verify listeners are registered
        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        Assertions.assertFalse(listenerMap
                .getExpressions("vaadin-geolocation-position").isEmpty());
        Assertions.assertFalse(listenerMap
                .getExpressions("vaadin-geolocation-error").isEmpty());

        // Drain any pending JS from track() setup
        ui.dumpPendingJsInvocations();

        // Detach the component
        ui.remove(component);

        // Verify listeners are removed after detach
        Assertions.assertTrue(listenerMap
                .getExpressions("vaadin-geolocation-position").isEmpty());
        Assertions.assertTrue(listenerMap
                .getExpressions("vaadin-geolocation-error").isEmpty());

        // Verify clearWatch JS was queued via Page.executeJs
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().anyMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_removesListenersAndQueuesClearWatch() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);
        Assertions.assertFalse(listenerMap
                .getExpressions("vaadin-geolocation-position").isEmpty());

        // Drain the pending JS from track() setup
        ui.dumpPendingJsInvocations();

        tracker.stop();

        Assertions.assertTrue(listenerMap
                .getExpressions("vaadin-geolocation-position").isEmpty());
        Assertions.assertTrue(listenerMap
                .getExpressions("vaadin-geolocation-error").isEmpty());

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().anyMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_isIdempotent() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);
        ui.dumpPendingJsInvocations();

        tracker.stop();
        ui.dumpPendingJsInvocations();

        // Second call must not queue another clearWatch
        Assertions.assertDoesNotThrow(tracker::stop);
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().noneMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.clearWatch")));
    }

    @Test
    void stop_afterDetach_isNoOp() {
        TestComponent component = new TestComponent();
        ui.add(component);

        GeolocationTracker tracker = ui.getGeolocation().track(component);
        ui.remove(component);
        ui.dumpPendingJsInvocations();

        Assertions.assertDoesNotThrow(tracker::stop);
        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().noneMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.clearWatch")));
    }

    // --- isSupported() / queryPermission() tests ---

    @Test
    void isSupported_executesJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().isSupported(supported -> {
        });

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(
                invocations.stream().anyMatch(inv -> inv.getInvocation()
                        .getExpression().contains("geolocation.isSupported")));
    }

    @Test
    void queryPermission_executesJs() {
        TestComponent component = new TestComponent();
        ui.add(component);

        ui.getGeolocation().queryPermission(state -> {
        });

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        Assertions.assertTrue(invocations.stream()
                .anyMatch(inv -> inv.getInvocation().getExpression()
                        .contains("geolocation.queryPermission")));
    }

    // --- GeolocationOptions builder tests ---

    @Test
    void geolocationOptions_builder_convertsDurationsToMillis() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(java.time.Duration.ofSeconds(10))
                .maximumAge(java.time.Duration.ZERO).build();

        Assertions.assertEquals(Boolean.TRUE, opts.enableHighAccuracy());
        Assertions.assertEquals(10_000, opts.timeout());
        Assertions.assertEquals(0, opts.maximumAge());
    }

    @Test
    void geolocationOptions_builder_leavesUnsetFieldsNull() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .timeout(java.time.Duration.ofSeconds(5)).build();

        Assertions.assertNull(opts.enableHighAccuracy());
        Assertions.assertEquals(5_000, opts.timeout());
        Assertions.assertNull(opts.maximumAge());
    }

    private void fireEvent(Element element, String eventType,
            ObjectNode eventData) {
        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);
        listenerMap.fireEvent(new DomEvent(element, eventType, eventData));
    }
}
