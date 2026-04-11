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
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Obtains the end user's physical location from the browser.
 * <p>
 * Every entry point on this class is asynchronous: calling it enqueues a
 * request to the browser and returns immediately. The browser answers later
 * (after the user responds to a permission prompt, after the operating system
 * reports a position, or after a timeout), and Flow invokes the callback or
 * updates the {@link Signal} on the UI thread. Application code therefore never
 * needs {@code ui.access(...)}.
 * <p>
 * <b>When can this be called?</b> Any of the static methods must be called
 * while a {@link UI} is current — typically from a click listener, an attach
 * listener, or a similar event callback. Calling from a background thread
 * without {@link UI#getCurrent()} will fail.
 * <p>
 * <b>Two usage modes:</b>
 * <ul>
 * <li>{@link #get(SerializableConsumer)} — one-shot position request. Use this
 * when the application only needs to know the user's location at a single
 * moment (e.g. on a button click).</li>
 * <li>{@link #track(Component)} — continuous tracking that keeps the server
 * updated as the user moves. The browser watch is automatically cancelled when
 * the owning component detaches, so the application does not need to write
 * cleanup code. Use {@link #stop()} to cancel the watch from application code
 * (for example from a "Stop tracking" button).</li>
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
 * or detect when it is shown. If the user denies the prompt the {@code onError}
 * callback receives a {@link GeolocationErrorCode#PERMISSION_DENIED} error.
 *
 * <p>
 * <b>One-shot example:</b>
 *
 * <pre>
 * Button locate = new Button("Use my location");
 * locate.addClickListener(
 *         e -&gt; Geolocation.get(
 *                 pos -&gt; showNearest(pos.coords().latitude(),
 *                         pos.coords().longitude()),
 *                 err -&gt; showManualEntry()));
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
     * Wrapper for the JS result which always resolves with either a position or
     * an error.
     */
    private record GetResult(GeolocationPosition position,
            GeolocationError error) implements Serializable {
    }

    private final ValueSignal<GeolocationState> stateSignal = new ValueSignal<>(
            new GeolocationState.Pending());

    // Tracking-handle state. Populated by track() and cleared by stop().
    private boolean active;
    private UI ui;
    private String watchKey;
    private DomListenerRegistration positionListener;
    private DomListenerRegistration errorListener;
    private Registration detachRegistration;

    private Geolocation() {
    }

    /**
     * Requests the user's current position once. Errors (including the user
     * denying the permission prompt) are silently ignored — use
     * {@link #get(SerializableConsumer, SerializableConsumer)} if the
     * application needs to react to failures.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds and a position is obtained,
     * {@code onSuccess} is invoked on the UI thread (no {@code ui.access(...)}
     * required).
     * <p>
     * Must be called while a {@link UI} is current (typically from a click
     * listener or other event callback).
     *
     * @param onSuccess
     *            invoked with the position once the browser reports it
     */
    public static void get(
            SerializableConsumer<GeolocationPosition> onSuccess) {
        get(null, onSuccess, null);
    }

    /**
     * Requests the user's current position once, with explicit success and
     * error callbacks.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, exactly one of {@code onSuccess}
     * or {@code onError} is invoked on the UI thread (no {@code ui.access(...)}
     * required). {@code onError} fires on permission denial, positioning
     * failure, and timeout — inspect {@link GeolocationError#errorCode()} to
     * distinguish the cases.
     * <p>
     * Must be called while a {@link UI} is current (typically from a click
     * listener or other event callback).
     *
     * @param onSuccess
     *            invoked with the position once the browser reports it
     * @param onError
     *            invoked if the browser reports an error, or {@code null} to
     *            silently ignore errors
     */
    public static void get(SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        get(null, onSuccess, onError);
    }

    /**
     * Requests the user's current position once with tuning options — use this
     * to trade accuracy for battery/speed or to accept a recent cached reading.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, exactly one of {@code onSuccess}
     * or {@code onError} is invoked on the UI thread (no {@code ui.access(...)}
     * required).
     * <p>
     * Must be called while a {@link UI} is current (typically from a click
     * listener or other event callback).
     *
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults. See {@link GeolocationOptions}
     * @param onSuccess
     *            invoked with the position once the browser reports it
     * @param onError
     *            invoked if the browser reports an error, or {@code null} to
     *            silently ignore errors
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
     * Starts continuously watching the user's position, tied to the owner
     * component's lifecycle.
     * <p>
     * The browser reports new positions whenever it detects movement. Each
     * report is delivered to the returned handle's {@link #state()} signal on
     * the UI thread. The initial state is {@link GeolocationState.Pending}
     * until the first reading arrives, then transitions to
     * {@link GeolocationPosition} (updated on every subsequent reading) or
     * {@link GeolocationError}.
     * <p>
     * The underlying browser watch is automatically cancelled when
     * {@code owner} detaches, so the application does not need to write cleanup
     * code for navigation. For cancelling while the view is still attached
     * (e.g. a "Stop tracking" button), call {@link #stop()} on the returned
     * handle.
     * <p>
     * The browser may show a permission dialog on the first call. If the user
     * denies, the signal transitions to {@link GeolocationError} with
     * {@link GeolocationErrorCode#PERMISSION_DENIED}.
     * <p>
     * Must be called while a {@link UI} is current.
     *
     * @param owner
     *            the component that owns this tracking session; detaching the
     *            component automatically stops the watch
     * @return a handle whose {@link #state()} reports progress and whose
     *         {@link #stop()} cancels the watch
     */
    public static Geolocation track(Component owner) {
        return track(owner, null);
    }

    /**
     * Starts continuously watching the user's position with tuning options,
     * tied to the owner component's lifecycle. Behaves like
     * {@link #track(Component)} but lets the caller request high accuracy, set
     * a failure timeout, or accept cached readings. See
     * {@link GeolocationOptions} for the available settings.
     * <p>
     * Must be called while a {@link UI} is current.
     *
     * @param owner
     *            the component that owns this tracking session; detaching the
     *            component automatically stops the watch
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults
     * @return a handle whose {@link #state()} reports progress and whose
     *         {@link #stop()} cancels the watch
     */
    public static Geolocation track(Component owner,
            GeolocationOptions options) {
        Geolocation geo = new Geolocation();
        Element el = owner.getElement();

        geo.active = true;
        geo.ui = owner.getUI().orElse(UI.getCurrent());
        geo.watchKey = UUID.randomUUID().toString();

        geo.positionListener = el
                .addEventListener("vaadin-geolocation-position",
                        e -> geo.stateSignal.set(
                                e.getEventDetail(GeolocationPosition.class)))
                .addEventDetail().allowInert();

        geo.errorListener = el
                .addEventListener("vaadin-geolocation-error",
                        e -> geo.stateSignal
                                .set(e.getEventDetail(GeolocationError.class)))
                .addEventDetail().allowInert();

        el.executeJs("window.Vaadin.Flow.geolocation.watch(this, $0, $1)",
                options, geo.watchKey);

        geo.detachRegistration = owner.addDetachListener(e -> geo.stop());

        return geo;
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
     * <p>
     * Must be called while a {@link UI} is current.
     *
     * @param callback
     *            invoked with {@code true} when geolocation is usable,
     *            {@code false} otherwise
     */
    public static void isSupported(SerializableConsumer<Boolean> callback) {
        UI ui = UI.getCurrent();
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
     * The callback receives one of:
     * <ul>
     * <li>{@link GeolocationPermission#GRANTED} — a subsequent {@link #get} or
     * {@link #track} call will proceed without a prompt.</li>
     * <li>{@link GeolocationPermission#DENIED} — permission was previously
     * denied; requests will fail with
     * {@link GeolocationErrorCode#PERMISSION_DENIED}. Use this to pre-explain
     * to the user why location is blocked.</li>
     * <li>{@link GeolocationPermission#PROMPT} — permission has not been
     * requested yet; the next call will show a dialog.</li>
     * <li>{@link GeolocationPermission#UNKNOWN} — the browser cannot report the
     * state. Treat this like "do nothing automatic".</li>
     * </ul>
     * <p>
     * <b>Safari caveat:</b> Safari does not implement permission querying for
     * geolocation and always returns {@code UNKNOWN}. Applications relying on
     * the {@code GRANTED} branch should therefore also provide an explicit user
     * action (e.g. a "Use my location" button) as a fallback for Safari users.
     * <p>
     * The call returns immediately. Some time later, {@code callback} is
     * invoked on the UI thread with the result.
     * <p>
     * Must be called while a {@link UI} is current.
     *
     * @param callback
     *            invoked with the current permission state
     */
    public static void queryPermission(
            SerializableConsumer<GeolocationPermission> callback) {
        UI ui = UI.getCurrent();
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

    /**
     * Returns a reactive signal that holds the most recent tracking state.
     * <p>
     * Combine with {@code ComponentEffect.effect(owner, ...)} to run code
     * whenever the state changes — the effect re-runs automatically on every
     * update and no manual event-listener bookkeeping is required. Outside an
     * effect, call {@code state().get()} or {@code state().peek()} to read a
     * snapshot.
     * <p>
     * The signal starts as {@link GeolocationState.Pending} and transitions to
     * {@link GeolocationPosition} on every successful reading, or
     * {@link GeolocationError} on failure. After {@link #stop()} (or after the
     * owner detaches), the last value remains readable but the signal stops
     * receiving updates.
     *
     * @return a read-only signal reporting the current state
     */
    public Signal<GeolocationState> state() {
        return stateSignal;
    }

    /**
     * Cancels continuous tracking started by
     * {@link #track(Component)}/{@link #track(Component, GeolocationOptions)}.
     * <p>
     * The browser stops reporting position updates and the {@link #state()}
     * signal stops changing. The last value remains readable. This is the way
     * to end tracking from application code (e.g. a "Stop" button) — leaving
     * the view automatically calls this method, so there is no need to call it
     * from a detach listener.
     * <p>
     * Idempotent and always safe: calling it twice, or calling it on a handle
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
