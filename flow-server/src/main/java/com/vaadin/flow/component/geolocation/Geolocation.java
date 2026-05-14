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
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;

/**
 * Browser geolocation API for Flow applications. Two entry points:
 * <ul>
 * <li>{@link #getPosition(SerializableConsumer, SerializableConsumer)} — read
 * the user's location once.</li>
 * <li>{@link #watchPosition(Component)} — keep receiving updates as the user
 * moves; returns a {@link GeolocationWatcher} handle that exposes both a
 * listener API and a reactive {@link Signal}.</li>
 * </ul>
 * Every call is asynchronous and returns immediately; the browser answers later
 * and Flow invokes the supplied consumers on the UI thread. The first call
 * shows the browser's permission dialog if no decision has been recorded yet;
 * if the user denies, the error consumer receives a {@link GeolocationError}
 * with code {@link GeolocationErrorCode#PERMISSION_DENIED}.
 *
 * <p>
 * <b>One-shot example:</b>
 *
 * <pre>
 * Button locate = new Button("Use my location");
 * locate.addClickListener(
 *         e -&gt; Geolocation.getPosition(
 *                 pos -&gt; showNearest(pos.coords().latitude(),
 *                         pos.coords().longitude()),
 *                 err -&gt; showManualEntry()));
 * </pre>
 *
 * <p>
 * <b>Continuous tracking with a listener (data-flow style):</b>
 *
 * <pre>
 * GeolocationWatcher watcher = Geolocation.watchPosition(this);
 * watcher.addPositionListener(pos -&gt; repository.save(pos), err -&gt; LOGGER
 *         .warn("location error: {} ({})", err.errorCode(), err.debugInfo()));
 * </pre>
 *
 * <p>
 * <b>Continuous tracking with a signal (reactive UI style):</b>
 *
 * <pre>
 * GeolocationWatcher watcher = Geolocation.watchPosition(this);
 * Signal&lt;GeolocationResult&gt; signal = watcher.positionSignal();
 *
 * status.bindText(signal.map(result -&gt; switch (result) {
 * case GeolocationPending p -&gt; "Waiting for first reading…";
 * case GeolocationError err -&gt; "Could not locate you.";
 * default -&gt; "";
 * }));
 *
 * Signal.effect(map, () -&gt; {
 *     if (signal.get() instanceof GeolocationPosition pos) {
 *         map.setCenter(new Coordinate(pos.coords().longitude(),
 *                 pos.coords().latitude()));
 *     }
 * });
 * </pre>
 */
public final class Geolocation implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Geolocation.class);

    private static final GeolocationOptions DEFAULT_OPTIONS = new GeolocationOptions(
            null, null, null);

    private Geolocation() {
        // utility class
    }

    /**
     * Requests the user's current position once, using the current UI.
     *
     * @param onSuccess
     *            invoked when the browser reports a position, never
     *            {@code null}
     * @param onError
     *            invoked when the browser reports an error, never {@code null}
     * @throws NullPointerException
     *             if either consumer is {@code null}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void getPosition(
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError) {
        getPosition(onSuccess, onError, DEFAULT_OPTIONS,
                UI.getCurrentOrThrow());
    }

    /**
     * Requests the user's current position once, using the current UI, with
     * tuning options. See {@link GeolocationOptions} for the available
     * settings.
     *
     * @param onSuccess
     *            invoked when the browser reports a position, never
     *            {@code null}
     * @param onError
     *            invoked when the browser reports an error, never {@code null}
     * @param options
     *            accuracy / timeout / cache-age tuning, never {@code null};
     *            pass an empty instance via
     *            {@code GeolocationOptions.builder().build()} to use browser
     *            defaults
     * @throws NullPointerException
     *             if any argument is {@code null}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void getPosition(
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError,
            GeolocationOptions options) {
        getPosition(onSuccess, onError, options, UI.getCurrentOrThrow());
    }

    /**
     * Requests the user's current position once on the given UI. Use this
     * overload from background threads or anywhere {@link UI#getCurrent()} is
     * unreliable.
     *
     * @param onSuccess
     *            invoked when the browser reports a position, never
     *            {@code null}
     * @param onError
     *            invoked when the browser reports an error, never {@code null}
     * @param ui
     *            the UI to dispatch the request through, never {@code null}
     * @throws NullPointerException
     *             if any argument is {@code null}
     */
    public static void getPosition(
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError, UI ui) {
        getPosition(onSuccess, onError, DEFAULT_OPTIONS, ui);
    }

    /**
     * Requests the user's current position once on the given UI with tuning
     * options. Use this overload from background threads or anywhere
     * {@link UI#getCurrent()} is unreliable.
     *
     * @param onSuccess
     *            invoked when the browser reports a position, never
     *            {@code null}
     * @param onError
     *            invoked when the browser reports an error, never {@code null}
     * @param options
     *            accuracy / timeout / cache-age tuning, never {@code null};
     *            pass an empty instance via
     *            {@code GeolocationOptions.builder().build()} to use browser
     *            defaults
     * @param ui
     *            the UI to dispatch the request through, never {@code null}
     * @throws NullPointerException
     *             if any argument is {@code null}
     */
    public static void getPosition(
            SerializableConsumer<GeolocationPosition> onSuccess,
            SerializableConsumer<GeolocationError> onError,
            GeolocationOptions options, UI ui) {
        Objects.requireNonNull(onSuccess, "onSuccess must not be null");
        Objects.requireNonNull(onError, "onError must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(ui, "ui must not be null");
        client(ui).get(options).whenComplete((outcome, error) -> {
            if (error != null) {
                LOGGER.debug("Geolocation getPosition() failed", error);
                deliverSafely(ui,
                        () -> onError.accept(new GeolocationError(
                                GeolocationErrorCode.UNKNOWN.code(),
                                "Client-side geolocation bridge failure")));
                return;
            }
            if (outcome instanceof GeolocationPosition position) {
                deliverSafely(ui, () -> onSuccess.accept(position));
            } else if (outcome instanceof GeolocationError errorOutcome) {
                deliverSafely(ui, () -> onError.accept(errorOutcome));
            }
        });
    }

    /**
     * Starts continuously watching the user's position, tied to the owner
     * component's lifecycle. The browser reports new positions as the device
     * moves; consume them via
     * {@link GeolocationWatcher#addPositionListener(SerializableConsumer, SerializableConsumer)}
     * for callbacks or {@link GeolocationWatcher#positionSignal()} for a
     * reactive signal. The watch is cancelled automatically when {@code owner}
     * detaches; call {@link GeolocationWatcher#stop()} to cancel sooner and
     * {@link GeolocationWatcher#resume()} to resume.
     * <p>
     * The watch starts as soon as {@code owner} is attached to a UI, so this
     * method is safe to call from a view constructor: if the component is
     * already attached the watch starts immediately, otherwise it starts on
     * first attach.
     * <p>
     * <b>Permission-revoke caveat.</b> If the user revokes geolocation
     * permission while a watch is active and then grants it again, the browser
     * silently stops delivering updates to the existing watch — this is the W3C
     * Geolocation API's documented behavior across browsers, not a
     * Flow-specific limitation. Recover by calling
     * {@link GeolocationWatcher#stop()} followed by
     * {@link GeolocationWatcher#resume()} to install a fresh browser watch;
     * subscribing to {@link #availabilityHintSignal()} can drive that
     * automatically.
     *
     * @param owner
     *            the component whose detach cancels the watch; the watch
     *            activates on the owner's first attach
     * @return a watcher exposing the position stream and a stop/resume handle
     * @throws NullPointerException
     *             if {@code owner} is {@code null}
     */
    public static GeolocationWatcher watchPosition(Component owner) {
        return watchPosition(owner, DEFAULT_OPTIONS);
    }

    /**
     * Starts continuously watching the user's position with tuning options,
     * tied to the owner component's lifecycle. Behaves like
     * {@link #watchPosition(Component)} but lets the caller request high
     * accuracy, set a failure timeout, or accept cached readings. See
     * {@link GeolocationOptions} for the available settings.
     *
     * @param owner
     *            the component whose detach cancels the watch; the watch
     *            activates on the owner's first attach
     * @param options
     *            accuracy / timeout / cache-age tuning, never {@code null};
     *            pass an empty instance via
     *            {@code GeolocationOptions.builder().build()} to use browser
     *            defaults
     * @return a watcher exposing the position stream and a stop/resume handle
     * @throws NullPointerException
     *             if either argument is {@code null}
     */
    public static GeolocationWatcher watchPosition(Component owner,
            GeolocationOptions options) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(options, "options must not be null");
        return new GeolocationWatcher(owner, options);
    }

    /**
     * Returns a read-only signal hinting at whether geolocation is usable for
     * the current UI. Useful for pre-rendering decisions like hiding a "Locate
     * me" button on insecure contexts or auto-fetching on return visits when
     * permission is already granted.
     * <p>
     * The value is best-effort and can be briefly stale:
     * <ul>
     * <li>Safari does not report permission state — every state surfaces as
     * {@link GeolocationAvailability#UNKNOWN UNKNOWN} (except
     * {@link GeolocationAvailability#UNSUPPORTED UNSUPPORTED}, which is always
     * reported correctly).</li>
     * <li>Firefox does not reliably propagate permission changes the user makes
     * in browser settings; the value can stay stale until the next
     * {@link #getPosition} or {@link #watchPosition} call.</li>
     * <li>Chromium has a small propagation delay between the browser permission
     * event and the cache update.</li>
     * </ul>
     * For authoritative results, call {@link #getPosition} and inspect the
     * outcome.
     *
     * @return the availability hint signal
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<GeolocationAvailability> availabilityHintSignal() {
        return availabilityHintSignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns a read-only signal hinting at whether geolocation is usable for
     * the given UI. Same semantics as {@link #availabilityHintSignal()}; use
     * this overload from background threads or anywhere {@link UI#getCurrent()}
     * is unreliable.
     *
     * @param ui
     *            the UI to read the hint from, never {@code null}
     * @return the availability hint signal
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static Signal<GeolocationAvailability> availabilityHintSignal(
            UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        // Ensure a client is installed so the signal is wired to the browser.
        client(ui);
        return ui.getInternals().getGeolocationAvailabilitySignal()
                .asReadonly();
    }

    /**
     * Runs {@code callback} and routes any {@link RuntimeException} it throws
     * to the session's {@link ErrorHandler}, matching the behavior of other
     * Flow listener APIs. Rethrows when no error handler is reachable (e.g.
     * during session teardown) so the exception is not silently lost.
     */
    static void deliverSafely(UI ui, Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException e) {
            VaadinSession session = ui.getSession();
            ErrorHandler handler = session == null ? null
                    : session.getErrorHandler();
            if (handler != null) {
                handler.error(new ErrorEvent(e));
            } else {
                throw e;
            }
        }
    }

    static GeolocationClient client(UI ui) {
        GeolocationClient existing = ui.getInternals().getGeolocationClient();
        if (existing != null) {
            return existing;
        }
        GeolocationClient client = resolveClient(ui);
        ui.getInternals().setGeolocationClient(client);
        return client;
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

}
