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
package com.vaadin.flow.component.wakelock;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.signals.Signal;

/**
 * Browser Screen Wake Lock API for Flow applications. Keeps the screen from
 * dimming or locking while the lock is held — useful for dashboards, kiosks,
 * presentations, and recipe / workout screens.
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * WakeLock.request();
 *
 * Signal.effect(this, () -&gt; {
 *     statusLabel.setText(
 *             WakeLock.activeSignal().get() ? "Screen will stay on" : "Idle");
 * });
 * </pre>
 *
 * <p>
 * <b>Lifecycle.</b> {@link #request()} fires the browser request asynchronously
 * and {@link #activeSignal()} flips to {@code true} once the browser confirms.
 * The browser releases the lock automatically when the tab is hidden; the
 * client transparently re-acquires it when the tab becomes visible again, so a
 * single {@link #request()} is enough for the lifetime of the view. Call
 * {@link #release()} to stop re-acquiring and drop the current lock.
 *
 * <p>
 * <b>Reliability caveats.</b>
 * <ul>
 * <li>Safari only ships the Screen Wake Lock API from version 16.4.</li>
 * <li>The browser requires a secure context (HTTPS or {@code localhost}); on an
 * insecure origin the call fails silently and {@link #activeSignal()} stays
 * {@code false}. Use {@link #availabilitySignal()} to gate UI controls on this
 * up front.</li>
 * <li>Some browsers release the lock on low battery or when the device enters
 * power-saving mode; {@link #activeSignal()} reflects this immediately.</li>
 * </ul>
 */
public final class WakeLock {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WakeLock.class);

    static final String STATE_CHANGE_EVENT = "vaadin-wake-lock-change";

    private WakeLock() {
        // utility class
    }

    /**
     * Asks the browser to acquire a screen wake lock for the current UI and
     * keep re-acquiring it across tab visibility changes.
     * <p>
     * The call is asynchronous and fire-and-forget: by the time this method
     * returns the browser has not necessarily granted the lock yet. Observe
     * {@link #activeSignal()} to react to the actual state. Calling
     * {@code request()} when a lock is already held is a no-op.
     *
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void request() {
        request(UI.getCurrentOrThrow());
    }

    /**
     * Asks the browser to acquire a screen wake lock for the given UI and keep
     * re-acquiring it across tab visibility changes. Use this overload from
     * background threads or anywhere {@link UI#getCurrent()} is unreliable.
     *
     * @param ui
     *            the UI to dispatch the request through, never {@code null}
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static void request(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        ensureWired(ui);
        ui.getElement().executeJs("window.Vaadin.Flow.wakeLock.request(this)")
                .then(ignored -> {
                }, err -> LOGGER
                        .debug("Client-side wakeLock.request failed: {}", err));
    }

    /**
     * Releases the screen wake lock on the current UI and stops re-acquiring it
     * on subsequent visibility changes. Idempotent — calling it when no lock is
     * held is a no-op.
     *
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void release() {
        release(UI.getCurrentOrThrow());
    }

    /**
     * Releases the screen wake lock on the given UI and stops re-acquiring it
     * on subsequent visibility changes. Use this overload from background
     * threads or anywhere {@link UI#getCurrent()} is unreliable.
     *
     * @param ui
     *            the UI to dispatch the release through, never {@code null}
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static void release(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        ensureWired(ui);
        ui.getElement().executeJs("window.Vaadin.Flow.wakeLock.release(this)")
                .then(ignored -> {
                }, err -> LOGGER
                        .debug("Client-side wakeLock.release failed: {}", err));
    }

    /**
     * Returns a read-only signal that reflects whether the browser is currently
     * holding the wake lock on behalf of the current UI.
     * <p>
     * Starts as {@code false}, flips to {@code true} when the browser confirms
     * a request, and flips back to {@code false} whenever the browser releases
     * the lock — explicitly through {@link #release()}, automatically when the
     * tab becomes hidden, or because the browser dropped it (power saving, low
     * battery). When the tab is shown again the client re-requests the lock if
     * {@link #release()} has not been called, so the signal flips back to
     * {@code true} shortly after.
     * <p>
     * Use {@code Signal.effect(owner, ...)} to react to changes and
     * {@code .peek()} for a snapshot outside a reactive context.
     *
     * @return the read-only active-state signal
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<Boolean> activeSignal() {
        return activeSignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns the read-only active-state signal for the given UI. Same
     * semantics as {@link #activeSignal()}; use this overload from background
     * threads or anywhere {@link UI#getCurrent()} is unreliable.
     *
     * @param ui
     *            the UI to read the signal from, never {@code null}
     * @return the read-only active-state signal
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static Signal<Boolean> activeSignal(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        ensureWired(ui);
        return ui.getInternals().getWakeLockActiveSignalReadOnly();
    }

    /**
     * Returns a read-only signal hinting at whether the Screen Wake Lock API is
     * usable in the current UI's page context. Useful for hiding a "keep screen
     * on" toggle entirely on insecure origins or browsers that do not implement
     * the API.
     * <p>
     * The value is seeded during bootstrap; the brief window between UI attach
     * and bootstrap completion surfaces as
     * {@link WakeLockAvailability#UNKNOWN}.
     *
     * @return the availability hint signal
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<WakeLockAvailability> availabilitySignal() {
        return availabilitySignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns the availability hint signal for the given UI. Same semantics as
     * {@link #availabilitySignal()}; use this overload from background threads
     * or anywhere {@link UI#getCurrent()} is unreliable.
     *
     * @param ui
     *            the UI to read the signal from, never {@code null}
     * @return the availability hint signal
     * @throws NullPointerException
     *             if {@code ui} is {@code null}
     */
    public static Signal<WakeLockAvailability> availabilitySignal(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        return ui.getInternals().getWakeLockAvailabilitySignalReadOnly();
    }

    private static void ensureWired(UI ui) {
        UIInternals internals = ui.getInternals();
        if (internals.isWakeLockListenerInstalled()) {
            return;
        }
        internals.markWakeLockListenerInstalled();
        ui.getElement().addEventListener(STATE_CHANGE_EVENT,
                e -> updateActive(internals, e.getEventDetail(String.class)))
                .addEventDetail().allowInert();
    }

    private static void updateActive(UIInternals internals,
            @Nullable String value) {
        if (value == null) {
            return;
        }
        switch (value) {
        case "ACTIVE" -> internals.setWakeLockActive(true);
        case "RELEASED" -> internals.setWakeLockActive(false);
        default ->
            LOGGER.debug("Unknown wake lock state from client: {}", value);
        }
    }
}
