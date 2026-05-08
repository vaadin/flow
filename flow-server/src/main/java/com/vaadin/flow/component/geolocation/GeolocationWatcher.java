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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A handle to a geolocation watch session, returned by
 * {@link Geolocation#watchPosition(Component)} and its overload.
 * <p>
 * Two ways to consume the stream of readings:
 * <ul>
 * <li>{@link #addPositionListener(SerializableConsumer, SerializableConsumer)}
 * — non-reactive callback pair. Convenient when the destination is a service,
 * repository, or anything that is not a UI component (e.g. a sports tracker
 * that writes points to a database).</li>
 * <li>{@link #positionSignal()} — reactive signal of {@link GeolocationResult}.
 * Convenient when binding component state to the position via
 * {@code Signal.effect} or {@code component.bindText(...)}.</li>
 * </ul>
 * The underlying browser watch is cancelled automatically when the owning
 * component detaches; call {@link #stop()} to cancel sooner and
 * {@link #resume()} to restart on the same handle. Bindings and listeners
 * survive stop/resume cycles.
 * <p>
 * The watch starts as soon as the owning component is attached to a UI: if the
 * component is already attached when the watcher is created the watch starts
 * immediately, otherwise it starts on first attach. Calling {@link #stop()}
 * before the first attach cancels the pending activation; the watcher can still
 * be activated later by calling {@link #resume()} on an attached owner.
 */
public class GeolocationWatcher implements Serializable {

    private final ValueSignal<GeolocationResult> positionSignal = new ValueSignal<>(
            new GeolocationPending());
    private final Signal<GeolocationResult> positionSignalReadOnly = positionSignal
            .asReadonly();

    private final ValueSignal<Boolean> activeSignal = new ValueSignal<>(
            Boolean.FALSE);
    private final Signal<Boolean> activeSignalReadOnly = activeSignal
            .asReadonly();

    private final List<PositionListener> listeners = new ArrayList<>();

    private final Component owner;
    private final GeolocationOptions options;

    private GeolocationClient.@Nullable WatchHandle handle;
    private @Nullable Registration detachRegistration;
    private @Nullable Registration pendingAttachActivation;

    GeolocationWatcher(Component owner, GeolocationOptions options) {
        this.owner = owner;
        this.options = options;
        if (owner.getUI().isPresent()) {
            resume();
        } else {
            pendingAttachActivation = owner.addAttachListener(e -> {
                cancelPendingAttachActivation();
                resume();
            });
        }
    }

    /**
     * Returns a read-only signal that holds the most recent reading.
     * <p>
     * Starts as {@link GeolocationPending} until the browser reports its first
     * value, then transitions to {@link GeolocationPosition} on every
     * successful reading or {@link GeolocationError} on failure. After
     * {@link #stop()} the signal stops receiving updates but its last value
     * stays readable; {@link #resume()} resets the value to
     * {@link GeolocationPending} and resumes updates. Subscribers stay attached
     * across stop/resume cycles.
     *
     * @return a read-only signal reporting the latest reading
     */
    public Signal<GeolocationResult> positionSignal() {
        return positionSignalReadOnly;
    }

    /**
     * Returns a read-only signal indicating whether the watcher is currently
     * receiving updates. Flips to {@code true} on {@link #resume()} and to
     * {@code false} on {@link #stop()} (or when the owner detaches). Useful for
     * binding a "Stop tracking" toggle's state without tracking a separate
     * flag.
     *
     * @return a read-only signal reporting whether the watch is active
     */
    public Signal<Boolean> activeSignal() {
        return activeSignalReadOnly;
    }

    /**
     * Subscribes to position and error pushes from the watch.
     * <p>
     * {@code onPosition} fires for every {@link GeolocationPosition} the
     * browser reports. {@code onError} fires for every {@link GeolocationError}
     * the browser reports. Neither fires for the initial
     * {@link GeolocationPending} state. Listeners stay attached across
     * {@link #stop()}/{@link #resume()} cycles; remove them through the
     * returned {@link Registration}.
     * <p>
     * Both consumers are required and must be non-null. To opt out of either
     * notification, pass {@code pos -> {}} or {@code err -> {}} explicitly.
     *
     * @param onPosition
     *            invoked on every successful reading, never {@code null}
     * @param onError
     *            invoked on every error reading, never {@code null}
     * @return a registration that removes both listeners when removed
     * @throws NullPointerException
     *             if either consumer is {@code null}
     */
    public Registration addPositionListener(
            SerializableConsumer<GeolocationPosition> onPosition,
            SerializableConsumer<GeolocationError> onError) {
        Objects.requireNonNull(onPosition, "onPosition must not be null");
        Objects.requireNonNull(onError, "onError must not be null");
        PositionListener listener = new PositionListener(onPosition, onError);
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Starts, or resumes, the underlying browser watch.
     * <p>
     * Called automatically when the owner is attached to a UI: immediately if
     * it is already attached when the watcher is created, otherwise on first
     * attach. Call again after {@link #stop()} to resume on the same handle —
     * bindings and listeners stay attached and start receiving updates again.
     * The signal resets to {@link GeolocationPending} on every resume. Calling
     * {@code resume()} on an already-running watcher is a no-op.
     *
     * @throws IllegalStateException
     *             if the owner is not attached to a UI
     */
    public void resume() {
        if (activeSignal.peek()) {
            return;
        }
        UI ui = owner.getUI()
                .orElseThrow(() -> new IllegalStateException(
                        "Owner component must be attached to a UI before "
                                + "resume() is called"));
        GeolocationClient client = Geolocation.client(ui);
        activeSignal.set(Boolean.TRUE);
        positionSignal.set(new GeolocationPending());

        handle = client.startWatch(owner, options, this::dispatch);
        detachRegistration = owner.addDetachListener(e -> stop());
    }

    /**
     * Cancels the underlying browser watch and tears down the server-side
     * subscriptions.
     * <p>
     * The browser stops reporting updates and {@link #positionSignal()} stops
     * changing. The last value remains readable. Detaching the owning component
     * calls this automatically, so most applications never need to call it from
     * a detach listener.
     * <p>
     * Idempotent: calling it twice, or after the owner has already detached,
     * does nothing extra. After {@code stop()} the watcher can be resumed with
     * {@link #resume()}.
     */
    public void stop() {
        cancelPendingAttachActivation();
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

    private void cancelPendingAttachActivation() {
        if (pendingAttachActivation != null) {
            pendingAttachActivation.remove();
            pendingAttachActivation = null;
        }
    }

    /**
     * Returns the active watch handle, or {@code null} if the watcher is not
     * currently active. Framework-internal seam used by external test drivers
     * to reach the underlying watch.
     *
     * @return the active watch handle, or {@code null} if stopped
     */
    GeolocationClient.@Nullable WatchHandle handle() {
        return handle;
    }

    private void dispatch(GeolocationResult result) {
        positionSignal.set(result);
        if (listeners.isEmpty()) {
            return;
        }
        UI ui = owner.getUI().orElseThrow();
        List<PositionListener> snapshot = new ArrayList<>(listeners);
        if (result instanceof GeolocationPosition position) {
            for (PositionListener listener : snapshot) {
                Geolocation.deliverSafely(ui,
                        () -> listener.onPosition.accept(position));
            }
        } else if (result instanceof GeolocationError error) {
            for (PositionListener listener : snapshot) {
                Geolocation.deliverSafely(ui,
                        () -> listener.onError.accept(error));
            }
        }
    }

    private record PositionListener(
            SerializableConsumer<GeolocationPosition> onPosition,
            SerializableConsumer<GeolocationError> onError)
            implements
                Serializable {
    }
}
