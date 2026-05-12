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
package com.vaadin.flow.component.page;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Browser Screen Wake Lock API for Flow applications. Keeps the screen from
 * dimming or locking while the lock is held — useful for dashboards, kiosks,
 * presentations, and recipe / workout screens.
 * <p>
 * Reach the per-UI instance through {@link Page#getWakeLock()}.
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * WakeLock wakeLock = UI.getCurrent().getPage().getWakeLock();
 * wakeLock.request();
 *
 * Signal.effect(this, () -&gt; {
 *     statusLabel.setText(
 *             wakeLock.activeSignal().get() ? "Screen will stay on" : "Idle");
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
 * {@code false}.</li>
 * <li>Some browsers release the lock on low battery or when the device enters
 * power-saving mode; {@link #activeSignal()} reflects this immediately.</li>
 * </ul>
 *
 * @see Page#getWakeLock()
 */
public final class WakeLock implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WakeLock.class);

    static final String STATE_CHANGE_EVENT = "vaadin-wake-lock-change";

    private final UI ui;
    private final ValueSignal<Boolean> activeSignal = new ValueSignal<>(
            Boolean.FALSE);
    private final Signal<Boolean> activeSignalReadOnly = activeSignal
            .asReadonly();

    WakeLock(UI ui) {
        this.ui = ui;
        ui.getElement()
                .addEventListener(STATE_CHANGE_EVENT,
                        e -> setActive(e.getEventDetail(String.class)))
                .addEventDetail().allowInert();
    }

    /**
     * Asks the browser to acquire a screen wake lock and keep re-acquiring it
     * across tab visibility changes.
     * <p>
     * The call is asynchronous and fire-and-forget: by the time this method
     * returns the browser has not necessarily granted the lock yet. Observe
     * {@link #activeSignal()} to react to the actual state. Calling
     * {@code request()} when a lock is already held is a no-op.
     */
    public void request() {
        ui.getElement().executeJs("window.Vaadin.Flow.wakeLock.request(this)")
                .then(ignored -> {
                }, err -> LOGGER
                        .debug("Client-side wakeLock.request failed: {}", err));
    }

    /**
     * Releases the screen wake lock and stops re-acquiring it on subsequent
     * visibility changes. Idempotent — calling it when no lock is held is a
     * no-op.
     */
    public void release() {
        ui.getElement().executeJs("window.Vaadin.Flow.wakeLock.release(this)")
                .then(ignored -> {
                }, err -> LOGGER
                        .debug("Client-side wakeLock.release failed: {}", err));
    }

    /**
     * Returns a read-only signal that reflects whether the browser is currently
     * holding the wake lock on behalf of this UI.
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
     */
    public Signal<Boolean> activeSignal() {
        return activeSignalReadOnly;
    }

    /**
     * Updates the signal from a raw state value reported by the client. Unknown
     * values are logged at debug level so a forward-compatible client value
     * does not silently disappear.
     *
     * @param value
     *            the raw value, or {@code null}
     */
    void setActive(String value) {
        if (value == null) {
            return;
        }
        switch (value) {
        case "ACTIVE" -> activeSignal.set(Boolean.TRUE);
        case "RELEASED" -> activeSignal.set(Boolean.FALSE);
        default ->
            LOGGER.debug("Unknown wake lock state from client: {}", value);
        }
    }
}
