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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.function.SerializableConsumer;

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
 * moment (e.g. on a button click). The callback receives a single
 * {@link GeolocationResult}; match on it to separate
 * {@link GeolocationPosition} from {@link GeolocationError}.</li>
 * <li>{@link #track(Component)} — continuous tracking that keeps the server
 * updated as the user moves. Returns a {@link GeolocationTracker} whose
 * {@link GeolocationTracker#value() value()} is a reactive signal of
 * {@link GeolocationResult}. The browser watch is automatically cancelled when
 * the owning component detaches; use {@link GeolocationTracker#stop()} to
 * cancel it sooner.</li>
 * </ul>
 * <b>Availability check:</b>
 * <ul>
 * <li>{@link #getAvailability()} — synchronous snapshot of whether the feature
 * is usable and what permission state the origin has. Kept in sync
 * automatically for the lifetime of the UI.</li>
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
 * locate.addClickListener(e -&gt; UI.getCurrent().getGeolocation().get(result -&gt; {
 *     switch (result) {
 *     case GeolocationPosition pos -&gt;
 *         showNearest(pos.coords().latitude(), pos.coords().longitude());
 *     case GeolocationError err -&gt; showManualEntry();
 *     }
 * }));
 * </pre>
 *
 * <p>
 * <b>Tracking example:</b>
 *
 * <pre>
 * GeolocationTracker tracker = UI.getCurrent().getGeolocation().track(this);
 * ComponentEffect.effect(this, () -&gt; {
 *     switch (tracker.value().get()) {
 *     case null -&gt; {
 *         // still waiting for the first reading
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
     * Wire shape of a one-shot get() answer: always exactly one of the two
     * result fields is populated, plus the updated availability.
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

    /**
     * Creates a new Geolocation facade bound to the given UI.
     * <p>
     * Application code obtains the instance via {@link UI#getGeolocation()} and
     * should not instantiate this class directly.
     *
     * @param ui
     *            the UI this facade belongs to
     */
    public Geolocation(UI ui) {
        this.ui = ui;
        // Listen for client-side permissionchange events so the cached
        // availability on ExtendedClientDetails stays current without
        // requiring a get()/track() call to refresh it.
        ui.getElement()
                .addEventListener("vaadin-geolocation-availability-change",
                        e -> setAvailability(
                                e.getEventDetail(AvailabilityDetail.class)
                                        .availability()))
                .addEventDetail().allowInert();
    }

    /**
     * Requests the user's current position once. The callback receives a
     * {@link GeolocationResult} that is either a {@link GeolocationPosition} or
     * a {@link GeolocationError}.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, the callback is invoked on the
     * UI thread.
     *
     * @param callback
     *            invoked with the outcome once the browser reports it
     */
    public void get(SerializableConsumer<GeolocationResult> callback) {
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
            SerializableConsumer<GeolocationResult> callback) {
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
                });
    }

    /**
     * Starts continuously watching the user's position, tied to the owner
     * component's lifecycle.
     * <p>
     * The browser reports new positions whenever it detects movement. Each
     * report is delivered to the returned tracker's
     * {@link GeolocationTracker#value() value()} signal on the UI thread. The
     * initial value is {@code null} until the first reading arrives, then
     * transitions to {@link GeolocationPosition} (updated on every subsequent
     * reading) or {@link GeolocationError}.
     * <p>
     * The underlying browser watch is automatically cancelled when
     * {@code owner} detaches, so the application does not need to write cleanup
     * code for navigation. For cancelling while the view is still attached
     * (e.g. a "Stop tracking" button), call {@link GeolocationTracker#stop()}
     * on the returned tracker.
     *
     * @param owner
     *            the component that owns this tracking session; detaching the
     *            component automatically stops the watch
     * @return a tracker whose {@link GeolocationTracker#value()} reports
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
     * @return a tracker whose {@link GeolocationTracker#value()} reports
     *         progress and whose {@link GeolocationTracker#stop()} cancels the
     *         watch
     */
    public GeolocationTracker track(Component owner,
            @Nullable GeolocationOptions options) {
        return new GeolocationTracker(ui, owner, options);
    }

    /**
     * Returns the current geolocation availability — whether the Geolocation
     * API is usable in this context and, if so, what permission state the
     * origin has.
     * <p>
     * Synchronous; can be read from {@code onAttach} without an async callback.
     * Stays in sync automatically as the user interacts with location or
     * changes site permissions. Returns {@code null} only in the atypical case
     * where the browser has not yet reported any value.
     *
     * @return the current availability, or {@code null} if never reported
     */
    public @Nullable GeolocationAvailability getAvailability() {
        ExtendedClientDetails details = ui.getInternals()
                .getExtendedClientDetails();
        return details == null ? null : details.getGeolocationAvailability();
    }

    /**
     * Internal: update the cached availability from a client-side response. The
     * availability is stored on {@link ExtendedClientDetails} so it shares a
     * location with other browser-provided details.
     */
    void setAvailability(@Nullable String value) {
        if (value == null) {
            return;
        }
        try {
            ui.getInternals().getExtendedClientDetails()
                    .setGeolocationAvailability(
                            GeolocationAvailability.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            // Unknown value — leave the previous cached value untouched.
        }
    }
}
