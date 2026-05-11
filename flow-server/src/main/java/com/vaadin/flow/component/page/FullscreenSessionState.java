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

import com.vaadin.flow.component.Component;

/**
 * Lifecycle state of a single {@link FullscreenSession}.
 * <p>
 * Each call to {@link Page#requestFullscreen()} or
 * {@link Component#requestFullscreen()} starts a new session in
 * {@link #PENDING} and ends in one of the four terminal states:
 * {@link #REJECTED} if the browser refused the request, {@link #EXITED_BY_CODE}
 * if server code initiated the exit, or {@link #EXITED_BY_USER} if the browser
 * exited fullscreen for any other reason (Escape key, browser close button,
 * navigation, etc.). The intermediate {@link #ACTIVE} state means the browser
 * accepted the request and fullscreen is currently in effect for this session.
 *
 * @see FullscreenSession#stateSignal()
 */
public enum FullscreenSessionState {

    /**
     * The request has been sent to the browser but no outcome has been reported
     * yet. Sessions transition out of this state to either {@link #ACTIVE} or
     * {@link #REJECTED}.
     */
    PENDING,

    /**
     * The browser has accepted the request and the page or component is
     * currently fullscreen. Sessions in this state transition to
     * {@link #EXITED_BY_CODE} or {@link #EXITED_BY_USER} when fullscreen ends.
     */
    ACTIVE,

    /**
     * The browser refused the request. Common causes are missing transient user
     * activation (the request was not made from a direct user gesture), an
     * iframe permissions policy that blocks fullscreen, or the document being
     * detached. A {@code WARN} is logged with the browser-provided error
     * message. Terminal state.
     */
    REJECTED,

    /**
     * The user ended fullscreen — Escape key, the browser's exit button,
     * navigation, or any other browser-side cause that was not a server-side
     * {@link Page#exitFullscreen()} or {@link Component#exitFullscreen()} or
     * {@link FullscreenSession#exit()} call. Terminal state.
     */
    EXITED_BY_USER,

    /**
     * Server code ended fullscreen by calling {@link Page#exitFullscreen()},
     * {@link Component#exitFullscreen()}, or {@link FullscreenSession#exit()}.
     * Also reached when a new {@code requestFullscreen()} supersedes the
     * session before the previous one exited. Terminal state.
     */
    EXITED_BY_CODE
}
