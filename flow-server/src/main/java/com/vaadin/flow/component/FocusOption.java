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
 * Marker interface for focus options.
 * <p>
 * Implementations of this interface can be passed to
 * {@link Focusable#focus(FocusOption...)}.
 * <p>
 * See <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
 * for more information.
 */
public interface FocusOption extends Serializable {

    /**
     * Builds an ObjectNode containing the focus options for use with the
     * browser's focus() method.
     * <p>
     * This method extracts FocusVisible and PreventScroll options from the
     * varargs and builds a JSON object compatible with the browser's
     * HTMLElement.focus() API. Returns null if all options are at their default
     * values.
     *
     * @param options
     *            zero or more focus options
     * @return an ObjectNode with the focus options, or null if all options are
     *         default
     */
    static ObjectNode buildOptions(FocusOption... options) {
        // Extract options from varargs
        FocusVisible focusVisible = FocusVisible.DEFAULT;
        PreventScroll preventScroll = PreventScroll.DEFAULT;

        for (FocusOption option : options) {
            if (option instanceof FocusVisible) {
                focusVisible = (FocusVisible) option;
            } else if (option instanceof PreventScroll) {
                preventScroll = (PreventScroll) option;
            }
        }

        // Build options object if any non-default values are specified
        if (preventScroll == PreventScroll.DEFAULT
                && focusVisible == FocusVisible.DEFAULT) {
            return null;
        }

        ObjectNode json = JacksonUtils.createObjectNode();

        if (preventScroll != PreventScroll.DEFAULT) {
            json.put("preventScroll", preventScroll == PreventScroll.ENABLED);
        }

        if (focusVisible != FocusVisible.DEFAULT) {
            json.put("focusVisible", focusVisible == FocusVisible.VISIBLE);
        }

        return json;
    }

    /**
     * Focus visibility option for focus operations.
     * <p>
     * Controls whether the browser should provide visible indication (focus
     * ring) that an element is focused.
     */
    enum FocusVisible implements FocusOption {
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
     * Scroll prevention option for focus operations.
     * <p>
     * Controls whether the browser should scroll the document to bring the
     * newly-focused element into view.
     */
    enum PreventScroll implements FocusOption {
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
}
