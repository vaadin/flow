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
package com.vaadin.flow.component.fullscreen;

/**
 * Represents the fullscreen state of a browser page.
 * <p>
 * Wraps the browser's Fullscreen API ({@code document.fullscreenEnabled} and
 * {@code document.fullscreenElement}) into four observable states: the browser
 * does not support fullscreen at all, fullscreen is supported but the page is
 * not in it, the page is currently fullscreen, and an {@link #UNKNOWN} sentinel
 * used before the first value has arrived from the client.
 *
 * @see Fullscreen#stateSignal()
 * @see Fullscreen#exit()
 * @since 25.2
 */
public enum FullscreenState {

    /**
     * No value has been reported by the browser yet. Used only as the initial
     * value of the signal before the first client handshake delivers the real
     * one. In normal request handling the signal is seeded before any user code
     * (UI initialization, {@code UIInitListener}, component attach) runs, so
     * this value is essentially never observed in practice; once a real value
     * has arrived, the signal never returns to {@code UNKNOWN}.
     */
    UNKNOWN,

    /**
     * The browser does not support fullscreen mode, or the document is not
     * permitted to enter it. In the browser, this corresponds to
     * {@code document.fullscreenEnabled} being {@code false}. Fullscreen
     * requests bound via {@link Fullscreen} resolve to a rejection in this
     * state.
     */
    UNSUPPORTED,

    /**
     * Fullscreen mode is supported and the page is currently not in it. In the
     * browser, this corresponds to {@code document.fullscreenEnabled} being
     * {@code true} and {@code document.fullscreenElement} being {@code null}.
     */
    NOT_FULLSCREEN,

    /**
     * The page is currently in fullscreen mode. In the browser, this
     * corresponds to {@code document.fullscreenElement} being non-{@code null}.
     */
    FULLSCREEN
}
