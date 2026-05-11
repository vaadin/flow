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
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A handle to a single fullscreen request, returned by
 * {@link Page#requestFullscreen()} and {@link Component#requestFullscreen()}.
 * <p>
 * A session is the unit that ties together one {@link FullscreenSessionState
 * lifecycle} (PENDING → ACTIVE or REJECTED → an EXITED state), the
 * {@linkplain #owner() owner component} that asked for fullscreen (empty when
 * the whole page was requested), and how it ended. Subscribe to
 * {@link #stateSignal()} to react to lifecycle transitions; call
 * {@link #exit()} to end the session from server code. The page-level
 * {@link Page#fullscreenSignal()} answers the cross-session "is anything
 * fullscreen right now" question and is independent of sessions.
 * <p>
 * Only one session can be active per page at a time. Starting a new request
 * while one is active transitions the old session to
 * {@link FullscreenSessionState#EXITED_BY_CODE EXITED_BY_CODE} before the new
 * session is returned.
 * <p>
 * If a request is rejected (no user activation, permissions policy, etc.) or
 * exits, the {@code error()} accessor returns the browser-provided error
 * message when available, otherwise empty.
 */
public class FullscreenSession implements Serializable {

    private final ValueSignal<FullscreenSessionState> stateSignal = new ValueSignal<>(
            FullscreenSessionState.PENDING);
    private final Signal<FullscreenSessionState> stateSignalReadOnly = stateSignal
            .asReadonly();
    private final @Nullable Component owner;
    private final Page page;
    private @Nullable String error;

    FullscreenSession(Page page, @Nullable Component owner) {
        this.page = page;
        this.owner = owner;
    }

    /**
     * Returns a read-only signal that tracks this session's
     * {@linkplain FullscreenSessionState lifecycle state}.
     * <p>
     * The signal starts at {@link FullscreenSessionState#PENDING} and
     * transitions through {@link FullscreenSessionState#ACTIVE} on a successful
     * request, or directly to a terminal state on rejection or exit. Once in a
     * terminal state the signal does not change again. Subscribe with
     * {@code Signal.effect(owner, ...)} to react to transitions.
     *
     * @return the read-only session state signal
     */
    public Signal<FullscreenSessionState> stateSignal() {
        return stateSignalReadOnly;
    }

    /**
     * Returns the component that requested fullscreen, or empty if the entire
     * page was requested via {@link Page#requestFullscreen()}. Useful for
     * binding the active component's appearance (e.g. an {@code expanded} style
     * class) only on the owner.
     *
     * @return the owner component, or empty for a page-level session
     */
    public Optional<Component> owner() {
        return Optional.ofNullable(owner);
    }

    /**
     * Returns the browser-provided error message when this session is in
     * {@link FullscreenSessionState#REJECTED REJECTED}, or empty otherwise.
     * Used for logging and surfacing diagnostics — the contents are
     * browser-defined and not stable enough to drive logic.
     *
     * @return the error message, when available
     */
    public Optional<String> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Ends this session. Equivalent to {@link Page#exitFullscreen()} when this
     * session is active; a no-op if the session already ended.
     */
    public void exit() {
        if (isTerminal()) {
            return;
        }
        page.exitFullscreen();
    }

    boolean isTerminal() {
        FullscreenSessionState state = stateSignal.peek();
        return state != FullscreenSessionState.PENDING
                && state != FullscreenSessionState.ACTIVE;
    }

    void setActive() {
        if (stateSignal.peek() == FullscreenSessionState.PENDING) {
            stateSignal.set(FullscreenSessionState.ACTIVE);
        }
    }

    void setRejected(@Nullable String errorMessage) {
        if (isTerminal()) {
            return;
        }
        this.error = errorMessage;
        stateSignal.set(FullscreenSessionState.REJECTED);
    }

    void setExited(boolean programmatic) {
        if (isTerminal()) {
            return;
        }
        stateSignal.set(programmatic ? FullscreenSessionState.EXITED_BY_CODE
                : FullscreenSessionState.EXITED_BY_USER);
    }
}
