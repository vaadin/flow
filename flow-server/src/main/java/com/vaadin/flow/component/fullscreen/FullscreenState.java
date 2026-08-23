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
 * The fullscreen state of the browser page, as reported by
 * {@link Fullscreen#stateSignal()}.
 * <p>
 * There are four observable states: the browser cannot go fullscreen at all
 * ({@link #UNSUPPORTED}), it can but the page is not currently fullscreen
 * ({@link #NOT_FULLSCREEN}), the page is fullscreen ({@link #FULLSCREEN}), and
 * an {@link #UNKNOWN} sentinel used before the browser has reported its first
 * value.
 *
 * @see Fullscreen#stateSignal()
 * @see Fullscreen#exit()
 * @since 25.2
 */
public enum FullscreenState {

    /**
     * The browser has not reported a value yet. This is only the signal's
     * initial value before the first client round-trip completes; in practice a
     * real value is in place before any application code runs, and the signal
     * never returns to {@code UNKNOWN} once it has a real value. Treat it as
     * "not known yet" if you ever observe it.
     */
    UNKNOWN,

    /**
     * The browser cannot go fullscreen — either it does not support fullscreen,
     * or the page is not permitted to enter it (for example when embedded in an
     * iframe without the required permission). A fullscreen request started via
     * {@link Fullscreen#onClick} fails in this state.
     */
    UNSUPPORTED,

    /**
     * The browser can go fullscreen and the page is currently not fullscreen.
     * This is the normal state before the user triggers a fullscreen request.
     */
    NOT_FULLSCREEN,

    /**
     * The page is currently displayed in fullscreen.
     */
    FULLSCREEN
}
