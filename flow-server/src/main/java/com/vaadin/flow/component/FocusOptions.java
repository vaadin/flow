/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.io.Serializable;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Options for controlling focus behavior.
 * <p>
 * See <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
 * for more information.
 */
public class FocusOptions implements Serializable {

    /**
     * Focus visibility option for {@link FocusOptions}.
     * <p>
     * Controls whether the browser should provide visible indication (focus
     * ring) that an element is focused.
     * <p>
     * See <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
     * for more information.
     */
    public enum FocusVisible {
        /**
         * Browser decides based on accessibility heuristics (default behavior).
         * <p>
         * When this option is used, the focusVisible property is not included
         * in the options passed to the browser, allowing the browser to
         * determine whether to show a focus ring based on how the focus was
         * triggered (e.g., keyboard vs mouse).
         */
        DEFAULT,

        /**
         * Force focus ring to be visible.
         * <p>
         * Use this to ensure a visible focus indicator is shown, which can
         * improve accessibility.
         */
        VISIBLE,

        /**
         * Force focus ring to NOT be visible.
         * <p>
         * Use this to prevent the focus ring from being shown. Use with caution
         * as this may impact accessibility.
         */
        NOT_VISIBLE
    }

    /**
     * Scroll prevention option for {@link FocusOptions}.
     * <p>
     * Controls whether the browser should scroll the document to bring the
     * newly-focused element into view.
     * <p>
     * See <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
     * for more information.
     */
    public enum PreventScroll {
        /**
         * Browser decides (default behavior is to scroll the element into
         * view).
         * <p>
         * When this option is used, the preventScroll property is not included
         * in the options passed to the browser, allowing the browser to use its
         * default behavior (which is to scroll).
         */
        DEFAULT,

        /**
         * Prevent scrolling when focusing the element.
         * <p>
         * Use this when you want to focus an element without changing the
         * current scroll position.
         */
        ENABLED,

        /**
         * Allow scrolling when focusing the element (browser default).
         * <p>
         * This explicitly enables the default browser behavior of scrolling the
         * element into view when focused.
         */
        DISABLED
    }

    private FocusVisible focusVisible = FocusVisible.DEFAULT;
    private PreventScroll preventScroll = PreventScroll.DEFAULT;

    /**
     * Create an instance with the default options.
     * <p>
     * Both focusVisible and preventScroll will use their DEFAULT values,
     * allowing the browser to decide the behavior.
     */
    public FocusOptions() {
    }

    /**
     * Create an instance with the given focus visibility option.
     * <p>
     * The preventScroll option will use its DEFAULT value.
     *
     * @param focusVisible
     *            the focus visibility option
     */
    public FocusOptions(FocusVisible focusVisible) {
        this.focusVisible = focusVisible;
    }

    /**
     * Create an instance with the given scroll prevention option.
     * <p>
     * The focusVisible option will use its DEFAULT value.
     *
     * @param preventScroll
     *            the scroll prevention option
     */
    public FocusOptions(PreventScroll preventScroll) {
        this.preventScroll = preventScroll;
    }

    /**
     * Create an instance with the given focus options.
     *
     * @param focusVisible
     *            the focus visibility option
     * @param preventScroll
     *            the scroll prevention option
     */
    public FocusOptions(FocusVisible focusVisible,
            PreventScroll preventScroll) {
        this.focusVisible = focusVisible;
        this.preventScroll = preventScroll;
    }

    /**
     * Sets the focus visibility option.
     *
     * @param focusVisible
     *            the focus visibility option
     */
    public void setFocusVisible(FocusVisible focusVisible) {
        this.focusVisible = focusVisible;
    }

    /**
     * Gets the focus visibility option.
     *
     * @return the focus visibility option
     */
    public FocusVisible getFocusVisible() {
        return focusVisible;
    }

    /**
     * Sets the scroll prevention option.
     *
     * @param preventScroll
     *            the scroll prevention option
     */
    public void setPreventScroll(PreventScroll preventScroll) {
        this.preventScroll = preventScroll;
    }

    /**
     * Gets the scroll prevention option.
     *
     * @return the scroll prevention option
     */
    public PreventScroll getPreventScroll() {
        return preventScroll;
    }

    /**
     * Convert to JSON ObjectNode in a form compatible with element.focus().
     * <p>
     * Only non-DEFAULT values are included in the JSON. If both options are
     * DEFAULT, null is returned.
     *
     * @return a JSON ObjectNode, or null if no options are set
     */
    public ObjectNode toJson() {
        ObjectNode json = JacksonUtils.createObjectNode();

        if (preventScroll != PreventScroll.DEFAULT) {
            json.put("preventScroll", preventScroll == PreventScroll.ENABLED);
        }

        if (focusVisible != FocusVisible.DEFAULT) {
            json.put("focusVisible", focusVisible == FocusVisible.VISIBLE);
        }

        return json.isEmpty() ? null : json;
    }
}
