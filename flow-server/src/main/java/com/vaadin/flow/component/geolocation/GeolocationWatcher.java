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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A handle to a geolocation watching session, returned by
 * {@link Geolocation#watchPosition(Component)} /
 * {@link Geolocation#watchPosition(Component, GeolocationOptions)}.
 * <p>
 * Exposes the latest {@link GeolocationResult} as a reactive signal via
 * {@link #valueSignal()}, and lets the application cancel watching via
 * {@link #stop()} or resume it via {@link #resume()}. The underlying browser
 * watch is also cancelled automatically when the owning component detaches, so
 * most applications never need to call {@code stop()} explicitly — it is
 * provided for "Stop watching" buttons and similar mid-view cancellation.
 * <p>
 * A watcher is reusable: after {@link #stop()} you can call {@link #resume()}
 * to resume watching on the same handle, and any effects or bindings subscribed
 * to {@link #valueSignal()} continue to work. Bind a toggle button's state to
 * {@link #activeSignal()} to let the UI react to start/stop without tracking
 * your own flag.
 */
public class GeolocationWatcher implements Serializable {

    private final ValueSignal<GeolocationResult> valueSignal = new ValueSignal<>(
            new GeolocationPending());
    private final Signal<GeolocationResult> valueSignalReadOnly = valueSignal
            .asReadonly();

    private final ValueSignal<Boolean> activeSignal = new ValueSignal<>(
            Boolean.FALSE);
    private final Signal<Boolean> activeSignalReadOnly = activeSignal
            .asReadonly();

    private final List<SerializableConsumer<GeolocationPosition>> positionListeners = new ArrayList<>();
    private final List<SerializableConsumer<GeolocationError>> errorListeners = new ArrayList<>();

    private final Component owner;
    private final @Nullable GeolocationOptions options;
    private final GeolocationClient client;

    private GeolocationClient.@Nullable WatchHandle handle;
    private @Nullable Registration detachRegistration;

    GeolocationWatcher(Component owner, @Nullable GeolocationOptions options,
            GeolocationClient client) {
        this.owner = owner;
        this.options = options;
        this.client = client;
        resume();
    }

    /**
     * Returns a read-only signal that holds the most recent watching result.
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
     * Returns a read-only signal that indicates whether the watcher is
     * currently receiving updates. Flips to {@code true} on {@link #resume()}
     * and to {@code false} on {@link #stop()} (or when the owner detaches).
     * <p>
     * Subscribe with {@code Signal.effect(owner, ...)} to bind a toggle
     * button's label/state to the watcher without tracking a separate flag.
     * Inside a reactive context, call {@code activeSignal().get()} to
     * subscribe; outside a reactive context, call {@code activeSignal().peek()}
     * for a snapshot.
     *
     * @return a read-only signal reporting whether watching is active
     */
    public Signal<Boolean> activeSignal() {
        return activeSignalReadOnly;
    }

    /**
     * Adds a listener pair that is notified on every reading the browser
     * reports. The listener-based equivalent of subscribing to
     * {@link #valueSignal()} for callers that prefer plain callbacks over
     * signals.
     * <p>
     * On every successful reading {@code onSuccess} is invoked with the
     * {@link GeolocationPosition}. If the browser reports an error instead
     * {@code onError} is invoked with the {@link GeolocationError}. The initial
     * {@link GeolocationPending} state is never delivered to listeners — they
     * only see real outcomes, mirroring the W3C
     * {@code watchPosition(success, error)} pair.
     * <p>
     * Listeners survive {@link #stop()} / {@link #resume()} cycles; remove them
     * via {@link Registration#remove()} on the returned registration. Both
     * callbacks are invoked on the UI thread.
     *
     * @param onSuccess
     *            invoked with each successful position reading; not
     *            {@code null}
     * @param onError
     *            invoked when the browser reports an error; not {@code null}
     * @return a registration that removes both listeners when called
     */
    public Registration addPositionListener(
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess listener cannot be null");
        Objects.requireNonNull(onError, "onError listener cannot be null");
        positionListeners.add(onSuccess);
        errorListeners.add(onError);
        return () -> {
            positionListeners.remove(onSuccess);
            errorListeners.remove(onError);
        };
    }

    /**
     * Starts, or resumes, the underlying browser watch.
     * <p>
     * Called automatically from the constructor so that a freshly created
     * watcher is immediately active. Call again after {@link #stop()} to resume
     * watching on the same handle — any effects or bindings subscribed to
     * {@link #valueSignal()} stay attached and start receiving new updates.
     * <p>
     * The signal is reset to {@link GeolocationPending} on every resume.
     * Calling {@code resume()} on an already-running watcher is a no-op.
     */
    public void resume() {
        if (activeSignal.peek()) {
            return;
        }
        activeSignal.set(Boolean.TRUE);
        valueSignal.set(new GeolocationPending());

        handle = client.startWatch(owner, options, this::handleResult);
        detachRegistration = owner.addDetachListener(e -> stop());
    }

    private void handleResult(GeolocationResult result) {
        valueSignal.set(result);
        switch (result) {
        case GeolocationPosition position -> {
            for (SerializableConsumer<GeolocationPosition> listener : new ArrayList<>(
                    positionListeners)) {
                listener.accept(position);
            }
        }
        case GeolocationError error -> {
            for (SerializableConsumer<GeolocationError> listener : new ArrayList<>(
                    errorListeners)) {
                listener.accept(error);
            }
        }
        case GeolocationPending pending -> {
            // Intentionally not dispatched to listeners — Pending is the
            // initial state set by resume(), not an outcome the W3C
            // watchPosition(success, error) pair would fire.
        }
        }
    }

    /**
     * Cancels the underlying browser watch and tears down the server-side
     * listeners.
     * <p>
     * The browser stops reporting position updates and {@link #valueSignal()}
     * stops changing. The last value remains readable. This is the way to end
     * watching from application code (e.g. a "Stop" button) — leaving the view
     * automatically calls this method, so there is no need to call it from a
     * detach listener.
     * <p>
     * Idempotent and always safe: calling it twice, or calling it on a watcher
     * whose owner has already detached, does nothing extra. After
     * {@code stop()} the watcher can be resumed with {@link #resume()}.
     */
    public void stop() {
        if (!activeSignal.peek()) {
            return;
        }
        activeSignal.set(Boolean.FALSE);
        if (handle != null) {
            handle.stop();
            handle = null;
        }
        if (detachRegistration != null) {
            detachRegistration.remove();
            detachRegistration = null;
        }
    }

    /**
     * Returns the active watch handle, or {@code null} if the watcher is not
     * currently active.
     *
     * @return the active watch handle, or {@code null} if the watcher has been
     *         stopped or auto-cancelled
     */
    GeolocationClient.@Nullable WatchHandle handle() {
        return handle;
    }
}
