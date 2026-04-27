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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A handle to a geolocation tracking session, returned by
 * {@link Geolocation#track(Component)} /
 * {@link Geolocation#track(Component, GeolocationOptions)}.
 * <p>
 * Exposes the latest {@link GeolocationResult} as a reactive signal via
 * {@link #valueSignal()}, and lets the application cancel tracking via
 * {@link #stop()} or resume it via {@link #resume()}. The underlying browser
 * watch is also cancelled automatically when the owning component detaches, so
 * most applications never need to call {@code stop()} explicitly — it is
 * provided for "Stop tracking" buttons and similar mid-view cancellation.
 * <p>
 * A tracker is reusable: after {@link #stop()} you can call {@link #resume()}
 * to resume tracking on the same handle, and any effects or bindings subscribed
 * to {@link #valueSignal()} continue to work. Bind a toggle button's state to
 * {@link #activeSignal()} to let the UI react to start/stop without tracking
 * your own flag.
 */
public class GeolocationTracker implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GeolocationTracker.class);

    private final ValueSignal<GeolocationResult> valueSignal = new ValueSignal<>(
            new GeolocationPending());
    private final Signal<GeolocationResult> valueSignalReadOnly = valueSignal
            .asReadonly();

    private final ValueSignal<Boolean> activeSignal = new ValueSignal<>(
            Boolean.FALSE);
    private final Signal<Boolean> activeSignalReadOnly = activeSignal
            .asReadonly();

    private final UI ui;
    private final Component owner;
    private final @Nullable GeolocationOptions options;
    private final String watchKey = UUID.randomUUID().toString();

    private @Nullable DomListenerRegistration positionListener;
    private @Nullable DomListenerRegistration errorListener;
    private @Nullable Registration detachRegistration;

    GeolocationTracker(UI ui, Component owner,
            @Nullable GeolocationOptions options) {
        this.ui = ui;
        this.owner = owner;
        this.options = options;
        resume();
    }

    /**
     * Returns a read-only signal that holds the most recent tracking result.
     * <p>
     * Combine with {@code Signal.effect(owner, ...)} or an attach listener to
     * run code whenever the value changes — the effect re-runs automatically on
     * every update and no manual event-listener bookkeeping is required. Inside
     * an effect or another reactive context, call {@code valueSignal().get()}
     * to read the current value and subscribe to further updates; outside a
     * reactive context, call {@code valueSignal().peek()} to read a snapshot
     * without subscribing.
     * <p>
     * The signal starts as {@link GeolocationPending} until the first reading
     * arrives, then transitions to {@link GeolocationPosition} on every
     * successful reading, or {@link GeolocationError} on failure. After
     * {@link #stop()} (or after the owner detaches), the last value remains
     * readable but the signal stops receiving updates. Calling
     * {@link #resume()} resumes updates; the signal is reset to
     * {@link GeolocationPending} on resume.
     *
     * @return a read-only signal reporting the latest result
     */
    public Signal<GeolocationResult> valueSignal() {
        return valueSignalReadOnly;
    }

    /**
     * Returns a read-only signal that indicates whether the tracker is
     * currently receiving updates. Flips to {@code true} on {@link #resume()}
     * and to {@code false} on {@link #stop()} (or when the owner detaches).
     * <p>
     * Subscribe with {@code Signal.effect(owner, ...)} to bind a toggle
     * button's label/state to the tracker without tracking a separate flag.
     * Inside a reactive context, call {@code activeSignal().get()} to
     * subscribe; outside a reactive context, call {@code activeSignal().peek()}
     * for a snapshot.
     *
     * @return a read-only signal reporting whether tracking is active
     */
    public Signal<Boolean> activeSignal() {
        return activeSignalReadOnly;
    }

    /**
     * Starts, or resumes, the underlying browser watch.
     * <p>
     * Called automatically from the constructor so that a freshly created
     * tracker is immediately active. Call again after {@link #stop()} to resume
     * tracking on the same handle — any effects or bindings subscribed to
     * {@link #valueSignal()} stay attached and start receiving new updates.
     * <p>
     * The signal is reset to {@link GeolocationPending} on every resume.
     * Calling {@code resume()} on an already-running tracker is a no-op.
     */
    public void resume() {
        if (activeSignal.peek()) {
            return;
        }
        activeSignal.set(Boolean.TRUE);
        valueSignal.set(new GeolocationPending());

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
                options, watchKey).then(ignored -> {
                }, err -> LOGGER.debug(
                        "Client-side geolocation.watch failed: {}", err));

        detachRegistration = owner.addDetachListener(e -> stop());
    }

    /**
     * Cancels the underlying browser watch and tears down the server-side
     * listeners.
     * <p>
     * The browser stops reporting position updates and {@link #valueSignal()}
     * stops changing. The last value remains readable. This is the way to end
     * tracking from application code (e.g. a "Stop" button) — leaving the view
     * automatically calls this method, so there is no need to call it from a
     * detach listener.
     * <p>
     * Idempotent and always safe: calling it twice, or calling it on a tracker
     * whose owner has already detached, does nothing extra. After
     * {@code stop()} the tracker can be resumed with {@link #resume()}.
     */
    public void stop() {
        if (!activeSignal.peek()) {
            return;
        }
        activeSignal.set(Boolean.FALSE);

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
        ui.getPage().executeJs("window.Vaadin.Flow.geolocation.clearWatch($0)",
                watchKey).then(ignored -> {
                }, err -> LOGGER.debug(
                        "Client-side geolocation.clearWatch failed: {}", err));
    }
}
