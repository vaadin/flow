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
package com.vaadin.flow.component.screenorientation;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.signals.Signal;

/**
 * Entry point for the browser <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/API/Screen_Orientation_API">Screen
 * Orientation API</a>.
 * <p>
 * To observe the current orientation, subscribe to
 * {@link #orientationSignal()}. To pin the screen to a particular orientation
 * for as long as the user stays on the current page, call {@link #lock} (most
 * browsers require fullscreen first); release it again with {@link #unlock}.
 *
 * <pre>{@code
 * Signal.effect(this, () -> {
 *     ScreenOrientationData data = ScreenOrientation.orientationSignal().get();
 *     setVisible(data.type().isPortrait());
 * });
 *
 * ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY);
 * }</pre>
 *
 * All methods operate on the {@link UI#getCurrent() current UI}; the no-UI
 * overloads require a current UI to be available on the calling thread.
 */
public final class ScreenOrientation implements Serializable {

    private static final String CHANGE_EVENT = "vaadin-screen-orientation-change";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ScreenOrientation.class);

    private ScreenOrientation() {
        // utility class
    }

    /**
     * Returns a read-only signal that tracks the current screen orientation and
     * its rotation angle for the current UI.
     * <p>
     * The signal is seeded from the initial client bootstrap, so user code
     * always sees a real value when the browser supports the Screen Orientation
     * API. Browsers that do not implement the API report
     * {@link ScreenOrientationType#UNSUPPORTED} after bootstrap; the initial
     * value before bootstrap is {@link ScreenOrientationType#UNKNOWN}. Once a
     * real value has arrived, the signal never returns to {@code UNKNOWN}.
     * <p>
     * Subscribe with {@code Signal.effect(owner, ...)} to react to changes;
     * call {@code orientationSignal().peek()} for a snapshot outside a reactive
     * context, and {@code .get()} inside one.
     *
     * @return the read-only screen orientation signal for the current UI
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static Signal<ScreenOrientationData> orientationSignal() {
        return orientationSignal(UI.getCurrentOrThrow());
    }

    /**
     * Returns the read-only screen orientation signal for the given UI. Same as
     * {@link #orientationSignal()} but without relying on the thread-bound
     * current UI — useful from framework code such as the bootstrap path.
     *
     * @param ui
     *            the UI whose orientation signal to return, not {@code null}
     * @return the read-only screen orientation signal for {@code ui}
     */
    public static Signal<ScreenOrientationData> orientationSignal(UI ui) {
        Objects.requireNonNull(ui, "ui must not be null");
        ensureWired(ui);
        return ui.getInternals().getScreenOrientationSignalReadOnly();
    }

    /**
     * Locks the screen orientation to the given type for as long as the user
     * remains on the current page. Most browsers require the document to be in
     * fullscreen mode, and locking is generally only honored on devices where a
     * physical orientation actually exists (mobile, tablet).
     * <p>
     * This overload is fire-and-forget: failures are logged at {@code DEBUG}
     * but not otherwise surfaced. Use
     * {@link #lock(ScreenOrientationType, SerializableRunnable, SerializableConsumer)}
     * to react to success or to the specific lock error.
     *
     * @param orientation
     *            the orientation to lock to, not {@code null} and not
     *            {@link ScreenOrientationType#UNKNOWN} or
     *            {@link ScreenOrientationType#UNSUPPORTED}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void lock(ScreenOrientationType orientation) {
        lock(orientation, () -> {
        }, error -> LOGGER.debug("Screen orientation lock failed: {} ({})",
                error.message(), error.name()));
    }

    /**
     * Locks the screen orientation to the given type and notifies the matching
     * callback when the browser resolves the request. Mirrors the
     * {@link com.vaadin.flow.component.geolocation.Geolocation#getPosition
     * Geolocation.getPosition} pattern so applications can bind UI to lock
     * success and failure without having to write JavaScript glue.
     * <p>
     * The browser dispatches exactly one of the two callbacks on the UI thread.
     * A lock typically requires fullscreen and a device that physically rotates
     * — see {@link ScreenOrientationLockError} for the {@code DOMException}
     * names you can expect on failure.
     *
     * @param orientation
     *            the orientation to lock to, not {@code null} and not
     *            {@link ScreenOrientationType#UNKNOWN} or
     *            {@link ScreenOrientationType#UNSUPPORTED}
     * @param onSuccess
     *            invoked when the browser confirms the lock; not {@code null}
     * @param onError
     *            invoked when the browser rejects the request, or when the
     *            Screen Orientation API is not available; not {@code null}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void lock(ScreenOrientationType orientation,
            SerializableRunnable onSuccess,
            SerializableConsumer<ScreenOrientationLockError> onError) {
        Objects.requireNonNull(orientation, "orientation cannot be null");
        Objects.requireNonNull(onSuccess, "onSuccess callback cannot be null");
        Objects.requireNonNull(onError, "onError callback cannot be null");
        if (orientation == ScreenOrientationType.UNKNOWN
                || orientation == ScreenOrientationType.UNSUPPORTED) {
            throw new IllegalArgumentException(
                    "Cannot lock to ScreenOrientationType."
                            + orientation.name());
        }
        UI.getCurrentOrThrow().getElement()
                .executeJs(
                        "return window.Vaadin.Flow.screenOrientation.lock($0)",
                        orientation.getClientValue())
                .then(LockResult.class, result -> {
                    if (result.success()) {
                        onSuccess.run();
                    } else {
                        onError.accept(new ScreenOrientationLockError(
                                result.name() == null ? "UnknownError"
                                        : result.name(),
                                result.message() == null ? ""
                                        : result.message()));
                    }
                }, bridgeError -> onError.accept(new ScreenOrientationLockError(
                        "BridgeError", bridgeError)));
    }

    /**
     * Releases a previous {@link #lock(ScreenOrientationType) lock}, allowing
     * the screen to follow the device orientation again. A no-op on browsers
     * that do not implement the Screen Orientation API.
     * <p>
     * Fire-and-forget: use {@link #unlock(SerializableRunnable)} to be notified
     * when the browser has applied the unlock.
     *
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void unlock() {
        UI.getCurrentOrThrow().getElement()
                .executeJs("window.Vaadin.Flow.screenOrientation.unlock()");
    }

    /**
     * Releases a previous {@link #lock(ScreenOrientationType) lock} and
     * notifies the given callback after the browser has applied the unlock. A
     * no-op (but the callback still fires) on browsers that do not implement
     * the Screen Orientation API.
     * <p>
     * Mirrors the callback shape of
     * {@link #lock(ScreenOrientationType, SerializableRunnable, SerializableConsumer)}
     * so cleanup flows ("leaving fullscreen — am I fully unlocked yet?") can be
     * sequenced reactively rather than assuming the unlock has landed.
     *
     * @param onComplete
     *            invoked on the UI thread once the unlock round-trip has
     *            completed; not {@code null}
     * @throws IllegalStateException
     *             if there is no current UI
     */
    public static void unlock(SerializableRunnable onComplete) {
        Objects.requireNonNull(onComplete,
                "onComplete callback cannot be null");
        UI.getCurrentOrThrow().getElement()
                .executeJs("window.Vaadin.Flow.screenOrientation.unlock()")
                .then(ignored -> onComplete.run());
    }

    /**
     * Sets the screen orientation of the given UI from raw client-side values
     * (e.g. from the initial bootstrap parameters). A {@code null} or empty
     * type leaves the current value untouched; the client reports
     * {@code "unsupported"} when the browser does not implement the Screen
     * Orientation API, which maps to {@link ScreenOrientationType#UNSUPPORTED}.
     * <p>
     * Called from the bootstrap path in {@code ExtendedClientDetails} that
     * seeds the signal before any user code observes it. Not intended for
     * application code.
     *
     * @param ui
     *            the UI whose orientation to seed, not {@code null}
     * @param type
     *            the raw orientation type from the client, or {@code null}
     * @param angle
     *            the raw orientation angle from the client, or {@code null}
     */
    public static void setStateFromClient(UI ui, @Nullable String type,
            @Nullable String angle) {
        Objects.requireNonNull(ui, "ui must not be null");
        ensureWired(ui);
        int angleValue;
        try {
            angleValue = angle == null ? 0 : Integer.parseInt(angle);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unparseable screen orientation angle from client: {}",
                    angle);
            return;
        }
        apply(ui, type, angleValue);
    }

    /**
     * Installs, once per UI, the DOM listener that bridges
     * {@code vaadin-screen-orientation-change} events into the signal held on
     * {@link UIInternals}. Guarded by a flag so repeated facade calls do not
     * attach duplicate listeners.
     */
    private static void ensureWired(UI ui) {
        UIInternals internals = ui.getInternals();
        if (internals.isScreenOrientationListenerInstalled()) {
            return;
        }
        internals.markScreenOrientationListenerInstalled();
        ui.getElement().addEventListener(CHANGE_EVENT,
                e -> apply(ui, e.getEventDetail(ScreenOrientationDetail.class)))
                .addEventDetail().allowInert();
    }

    private static void apply(UI ui, @Nullable ScreenOrientationDetail detail) {
        if (detail != null) {
            apply(ui, detail.type(), detail.angle());
        }
    }

    private static void apply(UI ui, @Nullable String type, int angle) {
        if (type == null || type.isEmpty()) {
            return;
        }
        try {
            ui.getInternals().setScreenOrientation(new ScreenOrientationData(
                    ScreenOrientationType.fromClientValue(type), angle));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown screen orientation type from client: {}",
                    type);
        }
    }

    private record ScreenOrientationDetail(@Nullable String type,
            int angle) implements Serializable {
    }

    private record LockResult(boolean success, @Nullable String name,
            @Nullable String message) implements Serializable {
    }
}
