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
 * Represents the visibility state of a browser page.
 * <p>
 * Uses the browser's Page Visibility API ({@code document.hidden}) combined
 * with {@code document.hasFocus()} to distinguish between three states: fully
 * visible and focused, visible but not focused (e.g. behind another window),
 * and hidden (e.g. background tab or minimized window).
 *
 * @see Page#pageVisibilitySignal()
 */
public enum PageVisibility {

    /**
     * The page is visible and focused.
     * <p>
     * In the browser, this means {@code !document.hidden} and
     * {@code document.hasFocus()} is {@code true}.
     */
    VISIBLE,

    /**
     * The page is visible but not focused, e.g. behind another window.
     * <p>
     * In the browser, this means {@code !document.hidden} and
     * {@code document.hasFocus()} is {@code false}.
     */
    VISIBLE_NOT_FOCUSED,

    /**
     * The page is not visible, e.g. the browser tab is in the background or the
     * window is minimized.
     * <p>
     * In the browser, this is indicated by {@code document.hidden} being
     * {@code true}.
     */
    HIDDEN
}
