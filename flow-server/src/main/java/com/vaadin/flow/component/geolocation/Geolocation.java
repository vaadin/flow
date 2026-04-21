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
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Per-{@link UI} facade for the browser's Geolocation API. Obtain via
 * {@link UI#getGeolocation()}.
 * <p>
 * Every entry point on this class is asynchronous: calling it enqueues a
 * request to the browser and returns immediately. The browser answers later
 * (after the user responds to a permission prompt, after the operating system
 * reports a position, or after a timeout), and Flow invokes the callback or
 * updates the signal on the UI thread. Application code therefore never needs
 * {@code ui.access(...)}.
 * <p>
 * <b>When can this be called?</b> Every method must be called while a
 * {@link UI} is current — typically from a click listener, an attach listener,
 * or a similar event callback. Calling from a background thread without a
 * current UI will fail.
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
 * <b>Two capability checks:</b>
 * <ul>
 * <li>{@link #isSupported(SerializableConsumer)} — returns {@code false} when
 * the feature is unusable in this context (insecure connection, or embedded in
 * an iframe that blocks geolocation). Use this to hide location controls
 * entirely.</li>
 * <li>{@link #queryPermission(SerializableConsumer)} — returns the current
 * browser permission state without triggering a prompt. Use this to decide
 * whether auto-fetching on page load will be silent (user has already granted)
 * or will pop up a permission dialog.</li>
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
 *     case GeolocationResult.Pending p -&gt; {
 *         // unreachable from get(), present so the switch is exhaustive
 *     }
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
 *     case GeolocationResult.Pending p -&gt; {
 *         // still waiting for the first fix
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
     * fields is populated.
     */
    private record GetResult(GeolocationPosition position,
            GeolocationError error) implements Serializable {
    }

    private final UI ui;

    /**
     * Creates a new Geolocation facade bound to the given UI.
     * <p>
     * Called from {@link UI}'s constructor; application code obtains the
     * instance via {@link UI#getGeolocation()} and should not instantiate this
     * class directly.
     *
     * @param ui
     *            the UI this facade belongs to
     */
    public Geolocation(UI ui) {
        this.ui = Objects.requireNonNull(ui, "ui");
    }

    /**
     * Requests the user's current position once. The callback receives a
     * {@link GeolocationResult} that is either a {@link GeolocationPosition} or
     * a {@link GeolocationError} — {@link GeolocationResult.Pending} never
     * appears here, but must still be handled to keep pattern-match switches
     * exhaustive.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, the callback is invoked on the
     * UI thread (no {@code ui.access(...)} required).
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
     * UI thread (no {@code ui.access(...)} required).
     *
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults
     * @param callback
     *            invoked with the outcome once the browser reports it
     */
    public void get(GeolocationOptions options,
            SerializableConsumer<GeolocationResult> callback) {
        Objects.requireNonNull(callback, "callback");
        ui.getElement()
                .executeJs("return window.Vaadin.Flow.geolocation.get($0)",
                        options)
                .then(GetResult.class, result -> {
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
     * initial value is {@link GeolocationResult.Pending} until the first
     * reading arrives, then transitions to {@link GeolocationPosition} (updated
     * on every subsequent reading) or {@link GeolocationError}.
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
            GeolocationOptions options) {
        Objects.requireNonNull(owner, "owner");
        return new GeolocationTracker(ui, owner, options);
    }

    /**
     * Asynchronously reports whether the geolocation feature can be used at all
     * in the current page context.
     * <p>
     * Returns {@code false} when the page is served over an insecure connection
     * (plain HTTP rather than HTTPS or {@code localhost}), or when the page is
     * embedded in an iframe whose Permissions-Policy blocks geolocation. In
     * either case the API is not merely denied — it is unusable regardless of
     * what the user chooses, so applications should hide location-related
     * controls entirely instead of showing a button that would always fail.
     * <p>
     * This is different from {@link #queryPermission}: {@code isSupported}
     * reports "can the API be used at all"; {@code queryPermission} reports
     * "has the user already granted/denied access".
     * <p>
     * The call returns immediately. Some time later, {@code callback} is
     * invoked on the UI thread with the result.
     *
     * @param callback
     *            invoked with {@code true} when geolocation is usable,
     *            {@code false} otherwise
     */
    public void isSupported(SerializableConsumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback");
        ui.getElement()
                .executeJs(
                        "return window.Vaadin.Flow.geolocation.isSupported()")
                .then(Boolean.class, callback::accept);
    }

    /**
     * Asynchronously reports the current geolocation permission state
     * <b>without</b> triggering a permission prompt.
     * <p>
     * Use this to decide whether a location request would be silent (the user
     * has already granted permission on a previous visit) or would pop up a
     * browser dialog. A typical pattern is auto-fetching a position on page
     * load only when the permission state is
     * {@link GeolocationPermission#GRANTED}, so first-time visitors are not
     * greeted by a surprise prompt.
     * <p>
     * <b>Safari caveat:</b> Safari does not implement permission querying for
     * geolocation and always returns {@link GeolocationPermission#UNKNOWN}.
     * Applications relying on the {@code GRANTED} branch should therefore also
     * provide an explicit user action (e.g. a "Use my location" button) as a
     * fallback for Safari users.
     * <p>
     * The call returns immediately. Some time later, {@code callback} is
     * invoked on the UI thread with the result.
     *
     * @param callback
     *            invoked with the current permission state
     */
    public void queryPermission(
            SerializableConsumer<GeolocationPermission> callback) {
        Objects.requireNonNull(callback, "callback");
        ui.getElement().executeJs(
                "return window.Vaadin.Flow.geolocation.queryPermission()")
                .then(String.class, result -> {
                    GeolocationPermission permission;
                    try {
                        permission = GeolocationPermission
                                .valueOf(result == null ? "UNKNOWN" : result);
                    } catch (IllegalArgumentException ex) {
                        permission = GeolocationPermission.UNKNOWN;
                    }
                    callback.accept(permission);
                });
    }
}
