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

/**
 * The visibility state of the browser page, as reported by
 * {@link Page#pageVisibilitySignal()}.
 * <p>
 * Distinguishes three observable states — the page is visible and focused
 * ({@link #VISIBLE}), visible but not focused ({@link #VISIBLE_NOT_FOCUSED}),
 * and hidden ({@link #HIDDEN}) — plus an {@link #UNKNOWN} sentinel used before
 * the browser has reported its first value. Use it to pause work the user
 * cannot see, such as polling or animations:
 *
 * <pre>{@code
 * Page page = UI.getCurrent().getPage();
 * Signal.effect(this, () -> {
 *     if (page.pageVisibilitySignal().get() == PageVisibility.HIDDEN) {
 *         pausePolling();
 *     } else {
 *         resumePolling();
 *     }
 * });
 * }</pre>
 *
 * @see Page#pageVisibilitySignal()
 * @since 25.2
 */
public enum PageVisibility {

    /**
     * The browser has not reported a value yet. This is only the signal's
     * initial value before the first client round-trip completes; in practice a
     * real value is in place before any application code runs, and the signal
     * never returns to {@code UNKNOWN} once it has a real value. Treat it as
     * "not known yet" if you ever observe it.
     */
    UNKNOWN,

    /**
     * The page is visible and currently has focus — the user is looking at this
     * tab and it is the active window.
     */
    VISIBLE,

    /**
     * The page is visible but does not have focus, for example when it sits
     * behind another window or the user is interacting with a different
     * application.
     */
    VISIBLE_NOT_FOCUSED,

    /**
     * The page is not visible, for example when its browser tab is in the
     * background or the window is minimized.
     */
    HIDDEN
}
