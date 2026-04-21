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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A handle to a running geolocation tracking session, returned by
 * {@link Geolocation#track(Component)} /
 * {@link Geolocation#track(Component, GeolocationOptions)}.
 * <p>
 * Exposes the latest {@link GeolocationResult} as a reactive signal via
 * {@link #value()}, and lets the application cancel tracking via
 * {@link #stop()}. The underlying browser watch is also cancelled automatically
 * when the owning component detaches, so most applications never need to call
 * {@code stop()} explicitly — it is provided for "Stop tracking" buttons and
 * similar mid-view cancellation.
 */
public class GeolocationTracker implements Serializable {

    private final ValueSignal<@Nullable GeolocationResult> valueSignal = new ValueSignal<>(
            (GeolocationResult) null);

    private final UI ui;
    private final String watchKey;

    private boolean active = true;
    private @Nullable DomListenerRegistration positionListener;
    private @Nullable DomListenerRegistration errorListener;
    private @Nullable Registration detachRegistration;

    GeolocationTracker(UI ui, Component owner,
            @Nullable GeolocationOptions options) {
        this.ui = ui;
        this.watchKey = UUID.randomUUID().toString();

        Element el = owner.getElement();

        positionListener = el
                .addEventListener("vaadin-geolocation-position",
                        e -> valueSignal.set(
                                e.getEventDetail(GeolocationPosition.class)))
                .addEventDetail().allowInert();

        errorListener = el
                .addEventListener("vaadin-geolocation-error",
                        e -> valueSignal
                                .set(e.getEventDetail(GeolocationError.class)))
                .addEventDetail().allowInert();

        el.executeJs("window.Vaadin.Flow.geolocation.watch(this, $0, $1)",
                options, watchKey);

        detachRegistration = owner.addDetachListener(e -> stop());
    }

    /**
     * Returns a read-only signal that holds the most recent tracking result.
     * <p>
     * Combine with {@code ComponentEffect.effect(owner, ...)} to run code
     * whenever the value changes — the effect re-runs automatically on every
     * update and no manual event-listener bookkeeping is required. Outside an
     * effect, call {@code value().get()} or {@code value().peek()} to read a
     * snapshot.
     * <p>
     * The signal starts as {@code null} until the first reading arrives, then
     * transitions to {@link GeolocationPosition} on every successful reading,
     * or {@link GeolocationError} on failure. After {@link #stop()} (or after
     * the owner detaches), the last value remains readable but the signal stops
     * receiving updates. Match with {@code case null} to handle the initial
     * waiting state in an exhaustive switch.
     *
     * @return a read-only signal reporting the latest result
     */
    public Signal<@Nullable GeolocationResult> value() {
        return valueSignal;
    }

    /**
     * Cancels the underlying browser watch and tears down the server-side
     * listeners.
     * <p>
     * The browser stops reporting position updates and {@link #value()} stops
     * changing. The last value remains readable. This is the way to end
     * tracking from application code (e.g. a "Stop" button) — leaving the view
     * automatically calls this method, so there is no need to call it from a
     * detach listener.
     * <p>
     * Idempotent and always safe: calling it twice, or calling it on a tracker
     * whose owner has already detached, does nothing extra.
     */
    public void stop() {
        if (!active) {
            return;
        }
        active = false;

        if (positionListener != null) {
            positionListener.remove();
            positionListener = null;
        }
        if (errorListener != null) {
            errorListener.remove();
            errorListener = null;
        }
        if (detachRegistration != null) {
            detachRegistration.remove();
            detachRegistration = null;
        }
        if (ui != null && watchKey != null) {
            ui.getPage().executeJs(
                    "window.Vaadin.Flow.geolocation.clearWatch($0)", watchKey);
        }
    }
}
