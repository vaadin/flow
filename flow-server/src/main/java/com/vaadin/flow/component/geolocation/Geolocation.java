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

import java.io.Serializable;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Provides access to the browser's Geolocation API.
 * <p>
 * Two usage modes are available:
 * <ul>
 * <li>{@link #get(SerializableConsumer)} for a one-shot position request with
 * callbacks</li>
 * <li>{@link #track(Component)} for continuous position tracking via a reactive
 * {@link Signal}, automatically tied to the owner component's lifecycle</li>
 * </ul>
 *
 * <p>
 * <b>One-shot example:</b>
 *
 * <pre>
 * Geolocation.get(pos -&gt; map.setCenter(pos.coords().latitude(),
 *         pos.coords().longitude()));
 * </pre>
 *
 * <p>
 * <b>Tracking example:</b>
 *
 * <pre>
 * Geolocation geo = Geolocation.track(this);
 * ComponentEffect.effect(this, () -&gt; {
 *     switch (geo.state().get()) {
 *     case GeolocationState.Pending p -&gt; {
 *     }
 *     case GeolocationPosition pos -&gt;
 *         map.setCenter(pos.coords().latitude(), pos.coords().longitude());
 *     case GeolocationError err -&gt; showError(err.message());
 *     }
 * });
 * </pre>
 */
public class Geolocation implements Serializable {

    /**
     * Wrapper for the JS result which always resolves with either a position or
     * an error.
     */
    private record GetResult(GeolocationPosition position,
            GeolocationError error) implements Serializable {
    }

    private final ValueSignal<GeolocationState> stateSignal = new ValueSignal<>(
            new GeolocationState.Pending());

    private Geolocation() {
    }

    /**
     * Requests the current position from the browser's Geolocation API. Errors
     * are silently ignored.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener). The callback runs in the UI thread, so there is no need to use
     * {@code ui.access()}.
     *
     * @param onSuccess
     *            called with the position when it becomes available
     */
    public static void get(
            SerializableConsumer<GeolocationPosition> onSuccess) {
        get(null, onSuccess);
    }

    /**
     * Requests the current position from the browser's Geolocation API with the
     * given options. Errors are silently ignored.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener). The callback runs in the UI thread, so there is no need to use
     * {@code ui.access()}.
     *
     * @param options
     *            the geolocation options, or {@code null} for browser defaults
     * @param onSuccess
     *            called with the position when it becomes available
     */
    public static void get(GeolocationOptions options,
            SerializableConsumer<GeolocationPosition> onSuccess) {
        get(options, onSuccess, null);
    }

    /**
     * Requests the current position from the browser's Geolocation API.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener). Callbacks run in the UI thread, so there is no need to use
     * {@code ui.access()}.
     *
     * @param onSuccess
     *            called with the position when it becomes available
     * @param onError
     *            called with a {@link GeolocationError} if the browser reports
     *            an error, or {@code null} to ignore errors
     */
    public static void get(SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        get(null, onSuccess, onError);
    }

    /**
     * Requests the current position from the browser's Geolocation API with the
     * given options.
     * <p>
     * Must be called from a UI thread (i.e., inside a request handler or event
     * listener). Callbacks run in the UI thread, so there is no need to use
     * {@code ui.access()}.
     *
     * @param options
     *            the geolocation options, or {@code null} for browser defaults
     * @param onSuccess
     *            called with the position when it becomes available
     * @param onError
     *            called with a {@link GeolocationError} if the browser reports
     *            an error, or {@code null} to ignore errors
     */
    public static void get(GeolocationOptions options,
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        UI ui = UI.getCurrent();
        ui.getElement()
                .executeJs("return window.Vaadin.Flow.geolocation.get($0)",
                        options)
                .then(GetResult.class, result -> {
                    if (result.position() != null) {
                        onSuccess.accept(result.position());
                    } else if (onError != null && result.error() != null) {
                        onError.accept(result.error());
                    }
                });
    }

    /**
     * Starts continuous position tracking, tied to the owner component's
     * lifecycle.
     * <p>
     * The tracking state is available through {@link #state()}, which starts as
     * {@link GeolocationState.Pending} and transitions to
     * {@link GeolocationPosition} or {@link GeolocationError} as the browser
     * reports updates. Tracking stops automatically when the owner component
     * detaches.
     *
     * @param owner
     *            the component whose lifecycle controls the tracking
     * @return a {@link Geolocation} instance with a reactive state signal
     */
    public static Geolocation track(Component owner) {
        return track(owner, null);
    }

    /**
     * Starts continuous position tracking with the given options, tied to the
     * owner component's lifecycle.
     * <p>
     * The tracking state is available through {@link #state()}, which starts as
     * {@link GeolocationState.Pending} and transitions to
     * {@link GeolocationPosition} or {@link GeolocationError} as the browser
     * reports updates. Tracking stops automatically when the owner component
     * detaches.
     *
     * @param owner
     *            the component whose lifecycle controls the tracking
     * @param options
     *            the geolocation options, or {@code null} for browser defaults
     * @return a {@link Geolocation} instance with a reactive state signal
     */
    public static Geolocation track(Component owner,
            GeolocationOptions options) {
        Geolocation geo = new Geolocation();
        Element el = owner.getElement();

        DomListenerRegistration posReg = el
                .addEventListener("vaadin-geolocation-position", e -> {
                    geo.stateSignal
                            .set(e.getEventDetail(GeolocationPosition.class));
                }).addEventDetail().allowInert();

        DomListenerRegistration errReg = el
                .addEventListener("vaadin-geolocation-error", e -> {
                    geo.stateSignal
                            .set(e.getEventDetail(GeolocationError.class));
                }).addEventDetail().allowInert();

        String watchKey = UUID.randomUUID().toString();
        el.executeJs("window.Vaadin.Flow.geolocation.watch(this, $0, $1)",
                options, watchKey);

        owner.addDetachListener(e -> {
            e.getUI().getPage().executeJs(
                    "window.Vaadin.Flow.geolocation.clearWatch($0)", watchKey);
            posReg.remove();
            errReg.remove();
        });

        return geo;
    }

    /**
     * Returns a signal holding the current tracking state. The signal starts as
     * {@link GeolocationState.Pending} and transitions to
     * {@link GeolocationPosition} or {@link GeolocationError} as the browser
     * reports updates.
     *
     * @return a read-only signal with the current geolocation state
     */
    public Signal<GeolocationState> state() {
        return stateSignal;
    }
}
