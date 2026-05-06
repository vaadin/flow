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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
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
 * <li>{@link #get(SerializableConsumer, SerializableConsumer)} — one-shot
 * position request. Use this when the application only needs to know the user's
 * location at a single moment (e.g. on a button click). Takes a pair of
 * callbacks — one for a successful {@link GeolocationPosition}, one for a
 * {@link GeolocationError} — mirroring the W3C
 * {@code getCurrentPosition(success, error)} pair and matching
 * {@link GeolocationTracker#addPositionListener
 * GeolocationTracker.addPositionListener}. An overload accepts a trailing
 * {@link GeolocationOptions} for accuracy / timeout / cache-age tuning.</li>
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
 *         e -&gt; UI.getCurrent().getGeolocation()
 *                 .get(pos -&gt; showNearest(pos.coords().latitude(),
 *                         pos.coords().longitude()),
 *                         err -&gt; showManualEntry()));
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

    private final UI ui;
    private final Signal<GeolocationAvailability> availabilityReadOnly;

    private GeolocationClient client;

    /**
     * Creates a new Geolocation facade bound to the given UI.
     * <p>
     * Framework-only. Application code obtains the instance via
     * {@link UI#getGeolocation()} and should not instantiate this class
     * directly — attempting to create a second instance for a UI that already
     * has one throws.
     * <p>
     * The underlying {@link GeolocationClient} is resolved through
     * {@link Lookup}: if a {@link GeolocationClientFactory} is registered, its
     * {@link GeolocationClientFactory#create(UI)} produces the client.
     * Otherwise the built-in browser-backed client is used.
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
        this.client = resolveClient(ui);
        wireClient(this.client);
    }

    private static GeolocationClient resolveClient(UI ui) {
        GeolocationClientFactory factory = lookupFactory(ui);
        if (factory != null) {
            return factory.create(ui);
        }
        GeolocationAvailability seed = ui.getInternals()
                .getGeolocationAvailabilitySignal().peek();
        if (seed == null) {
            seed = GeolocationAvailability.UNKNOWN;
        }
        return new BrowserGeolocationClient(ui, seed);
    }

    private static @Nullable GeolocationClientFactory lookupFactory(UI ui) {
        VaadinService service = VaadinService.getCurrent();
        if (service == null && ui.getSession() != null) {
            service = ui.getSession().getService();
        }
        if (service == null) {
            return null;
        }
        VaadinContext context = service.getContext();
        if (context == null) {
            return null;
        }
        Lookup lookup = context.getAttribute(Lookup.class);
        if (lookup == null) {
            return null;
        }
        return lookup.lookup(GeolocationClientFactory.class);
    }

    /**
     * Requests the user's current position once. On a successful reading
     * {@code onSuccess} is invoked with the {@link GeolocationPosition}; if the
     * browser reports an error instead {@code onError} is invoked with the
     * {@link GeolocationError}. The pair mirrors the W3C
     * {@code getCurrentPosition(success, error)} signature and matches
     * {@link GeolocationTracker#addPositionListener
     * GeolocationTracker.addPositionListener}, so callers can share the same
     * handler shape between one-shot and watch APIs.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, exactly one of the callbacks is
     * invoked on the UI thread.
     *
     * @param onSuccess
     *            invoked with the position on a successful reading; not
     *            {@code null}
     * @param onError
     *            invoked with the error if the browser reports one; not
     *            {@code null}
     */
    public void get(SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        get(onSuccess, onError, null);
    }

    /**
     * Requests the user's current position once with tuning options. Use this
     * to trade accuracy for battery/speed or to accept a recent cached reading.
     * See {@link GeolocationOptions} for the available settings.
     * <p>
     * The call returns immediately. The browser may show a permission dialog on
     * the first call; after the user responds, exactly one of the callbacks is
     * invoked on the UI thread.
     *
     * @param onSuccess
     *            invoked with the position on a successful reading; not
     *            {@code null}
     * @param onError
     *            invoked with the error if the browser reports one; not
     *            {@code null}
     * @param options
     *            accuracy / timeout / cache-age tuning, or {@code null} to use
     *            the browser defaults
     */
    public void get(SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError,
            @Nullable GeolocationOptions options) {
        Objects.requireNonNull(onSuccess, "onSuccess callback cannot be null");
        Objects.requireNonNull(onError, "onError callback cannot be null");
        client.get(options).whenComplete((outcome, error) -> {
            if (error != null) {
                LOGGER.debug("Geolocation get() failed", error);
                onError.accept(new GeolocationError(
                        GeolocationErrorCode.UNKNOWN.code(),
                        "Client-side geolocation bridge failure"));
                return;
            }
            switch (outcome) {
            case GeolocationPosition position -> onSuccess.accept(position);
            case GeolocationError outcomeError -> onError.accept(outcomeError);
            }
        });
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
        return new GeolocationTracker(owner, options, client);
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

    void setClient(GeolocationClient client) {
        this.client.close();
        this.client = client;
        wireClient(client);
    }

    private void wireClient(GeolocationClient client) {
        ui.getInternals()
                .setGeolocationAvailability(client.currentAvailability());
        client.subscribeAvailability(
                next -> ui.getInternals().setGeolocationAvailability(next));
    }
}
