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

import java.util.concurrent.CompletableFuture;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Provides access to the browser's Geolocation API.
 * <p>
 * Two usage modes are available:
 * <ul>
 * <li>{@link #get()} for a one-shot position request that returns a
 * {@link CompletableFuture}</li>
 * <li>{@link #track(Component)} for continuous position tracking via reactive
 * {@link Signal}s, automatically tied to the owner component's lifecycle</li>
 * </ul>
 *
 * <p>
 * <b>One-shot example:</b>
 *
 * <pre>
 * Geolocation.get().thenAccept(pos -&gt; map.setCenter(pos.coords().latitude(),
 *         pos.coords().longitude()));
 * </pre>
 *
 * <p>
 * <b>Tracking example:</b>
 *
 * <pre>
 * Geolocation geo = Geolocation.track(this);
 * ComponentEffect.effect(this, () -&gt; {
 *     GeolocationPosition pos = geo.value().get();
 *     if (pos != null) {
 *         map.setCenter(pos.coords().latitude(), pos.coords().longitude());
 *     }
 * });
 * </pre>
 */
public class Geolocation {

    private static final String GET_POSITION_PROMISE_JS = """
            return new Promise(function(resolve, reject) {
                navigator.geolocation.getCurrentPosition(
                    function(p) {
                        var ts = p.timestamp;
                        if ((Date.now() - ts) > 86400000000) { ts += 978307200000; }
                        resolve({
                            coords: {
                                latitude: p.coords.latitude,
                                longitude: p.coords.longitude,
                                accuracy: p.coords.accuracy,
                                altitude: p.coords.altitude,
                                altitudeAccuracy: p.coords.altitudeAccuracy,
                                heading: p.coords.heading,
                                speed: p.coords.speed
                            },
                            timestamp: ts
                        });
                    },
                    function(e) { reject(e.message); },
                    $0 || undefined
                );
            })""";

    private static final String WATCH_POSITION_JS = """
            if (this.__vaadinGeoWatchId != null) {
                navigator.geolocation.clearWatch(this.__vaadinGeoWatchId);
            }
            var el = this;
            this.__vaadinGeoWatchId = navigator.geolocation.watchPosition(
                function(p) {
                    var ts = p.timestamp;
                    if ((Date.now() - ts) > 86400000000) { ts += 978307200000; }
                    el.dispatchEvent(new CustomEvent('vaadin-geolocation-position', {
                        detail: {
                            coords: {
                                latitude: p.coords.latitude, longitude: p.coords.longitude,
                                accuracy: p.coords.accuracy, altitude: p.coords.altitude,
                                altitudeAccuracy: p.coords.altitudeAccuracy,
                                heading: p.coords.heading, speed: p.coords.speed
                            },
                            timestamp: ts
                        }
                    }));
                },
                function(e) {
                    el.dispatchEvent(new CustomEvent('vaadin-geolocation-error', {
                        detail: { code: e.code, message: e.message }
                    }));
                },
                $0 || undefined
            )""";

    private static final String CLEAR_WATCH_JS = """
            if (this.__vaadinGeoWatchId != null) {
                navigator.geolocation.clearWatch(this.__vaadinGeoWatchId);
                this.__vaadinGeoWatchId = null;
            }""";

    private final ValueSignal<GeolocationPosition> positionSignal = new ValueSignal<>();
    private final ValueSignal<GeolocationError> errorSignal = new ValueSignal<>();

    private Geolocation() {
    }

    /**
     * Requests the current position from the browser's Geolocation API.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener).
     *
     * @return a {@link CompletableFuture} that completes with the position, or
     *         completes exceptionally if the browser reports an error
     */
    public static CompletableFuture<GeolocationPosition> get() {
        return get(null);
    }

    /**
     * Requests the current position from the browser's Geolocation API with the
     * given options.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener).
     *
     * @param options
     *            the geolocation options, or {@code null} for browser defaults
     * @return a {@link CompletableFuture} that completes with the position, or
     *         completes exceptionally if the browser reports an error
     */
    public static CompletableFuture<GeolocationPosition> get(
            GeolocationOptions options) {
        UI ui = UI.getCurrent();
        return ui.getElement().executeJs(GET_POSITION_PROMISE_JS, options)
                .toCompletableFuture(GeolocationPosition.class);
    }

    /**
     * Starts continuous position tracking, tied to the owner component's
     * lifecycle.
     * <p>
     * Position updates are available through {@link #value()} and errors
     * through {@link #error()}. Tracking stops automatically when the owner
     * component detaches.
     *
     * @param owner
     *            the component whose lifecycle controls the tracking
     * @return a {@link Geolocation} instance with reactive signals for position
     *         and error
     */
    public static Geolocation track(Component owner) {
        return track(owner, null);
    }

    /**
     * Starts continuous position tracking with the given options, tied to the
     * owner component's lifecycle.
     * <p>
     * Position updates are available through {@link #value()} and errors
     * through {@link #error()}. Tracking stops automatically when the owner
     * component detaches.
     *
     * @param owner
     *            the component whose lifecycle controls the tracking
     * @param options
     *            the geolocation options, or {@code null} for browser defaults
     * @return a {@link Geolocation} instance with reactive signals for position
     *         and error
     */
    public static Geolocation track(Component owner,
            GeolocationOptions options) {
        Geolocation geo = new Geolocation();
        Element el = owner.getElement();

        DomListenerRegistration posReg = el
                .addEventListener("vaadin-geolocation-position", e -> {
                    geo.positionSignal
                            .set(e.getEventDetail(GeolocationPosition.class));
                    geo.errorSignal.set(null);
                }).addEventDetail().allowInert();

        DomListenerRegistration errReg = el
                .addEventListener("vaadin-geolocation-error", e -> {
                    geo.errorSignal
                            .set(e.getEventDetail(GeolocationError.class));
                }).addEventDetail().allowInert();

        el.executeJs(WATCH_POSITION_JS, options);

        owner.addDetachListener(e -> {
            el.executeJs(CLEAR_WATCH_JS);
            posReg.remove();
            errReg.remove();
        });

        return geo;
    }

    /**
     * Returns a signal that holds the latest position received from the
     * browser. The signal value is {@code null} until the first position update
     * arrives.
     *
     * @return a read-only signal with the current position
     */
    public Signal<GeolocationPosition> value() {
        return positionSignal;
    }

    /**
     * Returns a signal that holds the latest error received from the browser,
     * or {@code null} if no error has occurred or a successful position update
     * has been received since the last error.
     *
     * @return a read-only signal with the current error
     */
    public Signal<GeolocationError> error() {
        return errorSignal;
    }
}
