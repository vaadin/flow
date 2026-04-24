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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Signal;

/**
 * Facade for the browser's Geolocation API. Obtain via
 * {@link UI#getGeolocation()}.
 * <p>
 * Every entry point on this class is asynchronous: calling it enqueues a
 * request to the browser and returns immediately. The browser answers later
 * (after the user responds to a permission prompt, after the operating system
 * reports a position, or after a timeout), and Flow invokes the callback or
 * updates the signal on the UI thread.
 * <p>
 * <b>Two usage modes:</b>
 * <ul>
 * <li>{@link #get(SerializableConsumer)} — one-shot position request. Use this
 * when the application only needs to know the user's location at a single
 * moment (e.g. on a button click). The callback receives a
 * {@link GeolocationOutcome} — either a {@link GeolocationPosition} or a
 * {@link GeolocationError}.</li>
 * <li>{@link #track(Component)} — continuous tracking that keeps the server
 * updated as the user moves. Returns a {@link GeolocationTracker} whose
 * {@link GeolocationTracker#valueSignal() valueSignal()} is a reactive signal
 * of {@link GeolocationResult}. The browser watch is automatically cancelled
 * when the owning component detaches; use {@link GeolocationTracker#stop()} to
 * cancel it sooner and {@link GeolocationTracker#resume()} to resume.</li>
 * </ul>
 * <b>Availability check:</b>
 * <ul>
 * <li>{@link #availabilitySignal()} — reactive signal of whether the feature is
 * usable and what permission state the origin has. Subscribe with
 * {@code Signal.effect(owner, ...)} to react to changes, or call
 * {@code availabilitySignal().peek()} for a snapshot.</li>
 * </ul>
 *
 * <p>
 * <b>Permission prompts.</b> The first time the application asks for a
 * location, the browser shows its own permission dialog. The dialog is
 * controlled by the browser, not by Flow — Flow cannot style it, suppress it,
 * or detect when it is shown. If the user denies the prompt the callback
 * receives a {@link GeolocationError} whose {@link GeolocationError#errorCode()
 * errorCode} is {@link GeolocationErrorCode#PERMISSION_DENIED}.
 *
 * <p>
 * <b>One-shot example:</b>
 *
 * <pre>
 * Button locate = new Button("Use my location");
 * locate.addClickListener(
 *         e -&gt; UI.getCurrent().getGeolocation().get(outcome -&gt; {
 *             switch (outcome) {
 *             case GeolocationPosition pos -&gt; showNearest(
 *                     pos.coords().latitude(), pos.coords().longitude());
 *             case GeolocationError err -&gt; showManualEntry();
 *             }
 *         }));
 * </pre>
 *
 * <p>
 * <b>Tracking example:</b>
 *
 * <pre>
 * GeolocationTracker tracker = UI.getCurrent().getGeolocation().track(this);
 * Signal.effect(this, () -&gt; {
 *     switch (tracker.valueSignal().get()) {
 *     case GeolocationPending p -&gt; {
 *         // waiting for first reading
 *     }
 *     case GeolocationPosition pos -&gt;
 *         map.setCenter(new Coordinate(pos.coords().longitude(),
 *                 pos.coords().latitude()));
 *     case GeolocationError err -&gt; showError(err.message());
 *     }
 * });
 * </pre>
 */
public class Geolocation implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Geolocation.class);

    /**
     * Wire shape of a one-shot get() answer: always exactly one of the two
     * result fields is populated, plus the availability reported alongside so
     * the server can refresh the cache inline.
     */
    private record GetResult(@Nullable GeolocationPosition position,
            @Nullable GeolocationError error,
            @Nullable String availability) implements Serializable {
    }

    /**
     * Wire shape of a permission-change push from the client.
     */
    private record AvailabilityDetail(
            @Nullable String availability) implements Serializable {
    }

    private final UI ui;
    private final Signal<GeolocationAvailability> availabilityReadOnly;

    /**
     * Creates a new Geolocation facade bound to the given UI.
     * <p>
     * Framework-only. Application code obtains the instance via
     * {@link UI#getGeolocation()} and should not instantiate this class
     * directly — attempting to create a second instance for a UI that already
     * has one throws.
     *
     * @param ui
     *            the UI this facade belongs to
     * @throws IllegalStateException
     *             if the UI already has a Geolocation facade
     */
    public Geolocation(UI ui) {
        if (ui.getGeolocation() != null) {
            throw new IllegalStateException(
                    "A Geolocation facade has already been created for this "
                            + "UI. Use UI.getGeolocation() to obtain it.");
        }
        this.ui = ui;
        this.availabilityReadOnly = ui.getInternals()
                .getGeolocationAvailabilitySignal().asReadonly();
        // Listen for client-side permissionchange events so the cached
        // availability stays current without requiring a get()/track()
        // call to refresh it.
        ui.getElement()
                .addEventListener("vaadin-geolocation-availability-change",
                        e -> setAvailability(
                                e.getEventDetail(AvailabilityDetail.class)
                                        .availability()))
                .addEventDetail().allowInert();
    }

    /**
     * Requests the user's current position once. The callback receives a
     * {@link GeolocationOutcome} — either a {@link GeolocationPosition} or a
     * {@link GeolocationError}. Use {@code switch} pattern matching on the
     * outcome; no dead "pending" arm is needed because one-shot requests never
     * produce that value.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, the callback is invoked on the
     * UI thread.
     *
     * @param callback
     *            invoked with the outcome once the browser reports it
     */
    public void get(SerializableConsumer<GeolocationOutcome> callback) {
        get(null, callback);
    }

    /**
     * Requests the user's current position once with tuning options. Use this
     * to trade accuracy for battery/speed or to accept a recent cached reading.
     * See {@link GeolocationOptions} for the available settings.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, the callback is invoked on the
     * UI thread.
     *
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults
     * @param callback
     *            invoked with the outcome once the browser reports it
     */
    public void get(@Nullable GeolocationOptions options,
            SerializableConsumer<GeolocationOutcome> callback) {
        ui.getElement()
                .executeJs("return window.Vaadin.Flow.geolocation.get($0)",
                        options)
                .then(GetResult.class, result -> {
                    setAvailability(result.availability());
                    if (result.position() != null) {
                        callback.accept(result.position());
                    } else if (result.error() != null) {
                        callback.accept(result.error());
                    }
                }, err -> LOGGER.debug("Client-side geolocation.get failed: {}",
                        err));
    }

    /**
     * Starts continuously watching the user's position, tied to the owner
     * component's lifecycle.
     * <p>
     * The browser reports new positions whenever it detects movement. Each
     * report is delivered to the returned tracker's
     * {@link GeolocationTracker#valueSignal() valueSignal()} signal on the UI
     * thread. The initial value is {@link GeolocationPending} until the first
     * reading arrives, then transitions to {@link GeolocationPosition} (updated
     * on every subsequent reading) or {@link GeolocationError}.
     * <p>
     * The underlying browser watch is automatically cancelled when
     * {@code owner} detaches, so the application does not need to write cleanup
     * code for navigation. For cancelling while the view is still attached
     * (e.g. a "Stop tracking" button), call {@link GeolocationTracker#stop()}
     * on the returned tracker.
     * <p>
     * <b>Permission-revoke caveat.</b> If the user revokes geolocation
     * permission while a watch is active and then grants it again, the browser
     * silently stops delivering position updates to the existing watch — this
     * is the W3C Geolocation API's documented behavior across browsers, not a
     * Flow-specific limitation. To recover after a revoke/regrant cycle, call
     * {@link GeolocationTracker#stop()} followed by
     * {@link GeolocationTracker#resume()}, which installs a fresh browser
     * watch. Applications that want this to happen automatically can subscribe
     * to {@link #availabilitySignal()} with {@code Signal.effect(owner, ...)}
     * and trigger the stop/resume when the availability transitions back to
     * {@link GeolocationAvailability#GRANTED GRANTED}.
     *
     * @param owner
     *            the component that owns this tracking session; detaching the
     *            component automatically stops the watch
     * @return a tracker whose {@link GeolocationTracker#valueSignal()} reports
     *         progress and whose {@link GeolocationTracker#stop()} cancels the
     *         watch
     */
    public GeolocationTracker track(Component owner) {
        return track(owner, null);
    }

    /**
     * Starts continuously watching the user's position with tuning options,
     * tied to the owner component's lifecycle. Behaves like
     * {@link #track(Component)} but lets the caller request high accuracy, set
     * a failure timeout, or accept cached readings. See
     * {@link GeolocationOptions} for the available settings.
     *
     * @param owner
     *            the component that owns this tracking session; detaching the
     *            component automatically stops the watch
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults
     * @return a tracker whose {@link GeolocationTracker#valueSignal()} reports
     *         progress and whose {@link GeolocationTracker#stop()} cancels the
     *         watch
     */
    public GeolocationTracker track(Component owner,
            @Nullable GeolocationOptions options) {
        return new GeolocationTracker(ui, owner, options);
    }

    /**
     * Returns a read-only signal holding the current geolocation availability —
     * whether the Geolocation API is usable in this context and, if so, what
     * permission state the origin has.
     * <p>
     * Subscribe with {@code Signal.effect(owner, ...)} to react to availability
     * changes (e.g. disabling a location button when the user revokes
     * permission). For a snapshot read, call
     * {@code availabilitySignal().peek()}; in an effect or reactive context,
     * call {@code availabilitySignal().get()}.
     * <p>
     * The signal starts as {@link GeolocationAvailability#UNKNOWN UNKNOWN},
     * transitions to the value reported during the initial client bootstrap,
     * and updates on every {@link #get} / {@link #track} outcome and on browser
     * permission-change events where supported.
     * <p>
     * <b>Reliability caveats.</b> The value is best-effort, not authoritative —
     * it reflects what the browser last reported, and can be briefly stale in
     * these cases:
     * <ul>
     * <li>Between server attach and the completion of the first client
     * handshake — holds {@link GeolocationAvailability#UNKNOWN UNKNOWN} during
     * this short window, indistinguishable from a real UNKNOWN reported by the
     * browser.</li>
     * <li>On Safari, the permission state is never observable;
     * {@link GeolocationAvailability#GRANTED GRANTED},
     * {@link GeolocationAvailability#DENIED DENIED} and
     * {@link GeolocationAvailability#PROMPT PROMPT} all surface as
     * {@link GeolocationAvailability#UNKNOWN UNKNOWN}.
     * {@link GeolocationAvailability#UNSUPPORTED UNSUPPORTED} is still reported
     * correctly.</li>
     * <li>On Firefox, permission changes the user makes in browser settings are
     * not reliably propagated back — the signal can stay stale until the next
     * {@link #get} or {@link #track} call.</li>
     * <li>On Chromium, the value updates promptly when the user flips the site
     * permission, but there is still a small propagation delay between the
     * browser event and the cache update.</li>
     * </ul>
     * Treat the value as a hint for pre-rendering decisions (e.g. auto-fetching
     * on return visits, hiding controls in unsupported contexts). For critical
     * paths, call {@link #get} and handle the authoritative result in the
     * callback.
     *
     * @return the availability signal
     */
    public Signal<GeolocationAvailability> availabilitySignal() {
        return availabilityReadOnly;
    }

    private void setAvailability(@Nullable String value) {
        if (value == null) {
            return;
        }
        try {
            ui.getInternals().setGeolocationAvailability(
                    GeolocationAvailability.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            // Unknown value — leave the previous cached value untouched.
        }
    }
}
