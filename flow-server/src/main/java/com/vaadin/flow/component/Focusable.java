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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ObjectNode;

/**
 * Represents a component that can gain and lose focus.
 *
 * @param <T>
 *            the type of the component which implements the interface
 * @see BlurNotifier
 * @see FocusNotifier
 *
 * @author Vaadin Ltd.
 * @since 1.0
 */
public interface Focusable<T extends Component>
        extends HasElement, BlurNotifier<T>, FocusNotifier<T>, HasEnabled {

    /**
     * Marker interface for focus options.
     * <p>
     * Used to type-check options passed to {@link #focus(FocusOption...)}.
     */
    interface FocusOption {
    }

    /**
     * Focus visibility option for focus operations.
     * <p>
     * Controls whether the browser should provide visible indication (focus
     * ring) that an element is focused.
     * <p>
     * See <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
     * for more information.
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
     * <p>
     * See <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">HTMLElement.focus()</a>
     * for more information.
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

    /**
     * Sets the <code>tabindex</code> attribute in the component. The tabIndex
     * indicates if its element can be focused, and if/where it participates in
     * sequential keyboard navigation:
     * <ul>
     * <li>A negative value (usually <code>tabindex = -1</code> means that the
     * component should be focusable, but should not be reachable via sequential
     * keyboard navigation.</li>
     *
     * <li><code>tabindex = 0</code> means that the component should be
     * focusable in sequential keyboard navigation, but its order is defined by
     * the document's source order.</li>
     *
     * <li>A positive value means the component should be focusable in
     * sequential keyboard navigation, with its order defined by the value of
     * the number. That is, <code>tabindex = 4</code> would be focused before
     * <code>tabindex = 5</code>, but after <code>tabindex = 3</code>. If
     * multiple components share the same positive tabindex value, their order
     * relative to each other follows their position in the document
     * source.</li>
     * </ul>
     *
     * @param tabIndex
     *            the tabindex attribute
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/tabindex">tabindex
     *      at MDN</a>
     */
    default void setTabIndex(int tabIndex) {
        getElement().setAttribute("tabindex", String.valueOf(tabIndex));
    }

    /**
     * Gets the <code>tabindex</code> in the component. The tabIndex indicates
     * if its element can be focused, and if/where it participates in sequential
     * keyboard navigation.
     * <p>
     * If there's no such attribute set, it returns the default setting for the
     * element, which depends on the element and on the browser. If the
     * attribute cannot be parsed to <code>int</code>, then an
     * {@link IllegalStateException} is thrown.
     *
     * @return the tabindex attribute, or 0 if none
     * @throws IllegalStateException
     *             if the returned tabindex from the element is empty or can not
     *             be parsed to int
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/tabindex">tabindex
     *      at MDN</a>
     */
    default int getTabIndex() {
        String attribute = getElement().getAttribute("tabindex");
        if (attribute == null || attribute.isEmpty()) {
            throw new IllegalStateException(
                    "tabindex attribute is empty on element "
                            + getElement().getTag());
        }
        try {
            return Integer.parseInt(attribute);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "tabindex attribute could not be parsed on element "
                            + getElement().getTag() + ": " + attribute);
        }
    }

    /**
     * Calls the <code>focus</code> function at the client, making the component
     * keyboard focused.
     * <p>
     * This method can be called with no arguments for default browser behavior,
     * or with one or more {@link FocusOption} values to control focus behavior:
     * <ul>
     * <li>{@link FocusVisible} - controls whether the focus ring is visible</li>
     * <li>{@link PreventScroll} - controls whether the browser scrolls to the element</li>
     * </ul>
     * <p>
     * Examples:
     * <pre>
     * component.focus(); // Default behavior
     * component.focus(PreventScroll.ENABLED); // Focus without scrolling
     * component.focus(FocusVisible.VISIBLE, PreventScroll.ENABLED); // Both options
     * </pre>
     * <p>
     * Note: The {@code focusVisible} option is experimental and may not be
     * supported in all browsers. When not specified, the browser decides whether
     * to show the focus ring based on accessibility heuristics (e.g., keyboard
     * vs mouse interaction).
     *
     * @param options
     *            zero or more focus options
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/focus">focus
     *      at MDN</a>
     */
    default void focus(FocusOption... options) {
        Element element = getElement();

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
        ObjectNode json = null;
        if (preventScroll != PreventScroll.DEFAULT
                || focusVisible != FocusVisible.DEFAULT) {
            json = JacksonUtils.createObjectNode();

            if (preventScroll != PreventScroll.DEFAULT) {
                json.put("preventScroll",
                        preventScroll == PreventScroll.ENABLED);
            }

            if (focusVisible != FocusVisible.DEFAULT) {
                json.put("focusVisible", focusVisible == FocusVisible.VISIBLE);
            }
        }

        if (json == null) {
            // No options, call focus() without arguments
            element.executeJs("setTimeout(function(){$0.focus()},0)", element);
        } else {
            // Call focus with options object passed as parameter
            element.executeJs("setTimeout(function(){$0.focus($1)},0)", element,
                    json);
        }
    }

    /**
     * Calls the <code>blur</code> function at the client, making the component
     * lose keyboard focus.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/blur">blur
     *      at MDN</a>
     */
    default void blur() {
        getElement().callJsFunction("blur");
    }

    /**
     * Adds a shortcut which focuses the {@link Component} which implements
     * {@link Focusable} interface. The shortcut's event listener is in global
     * scope and the shortcut's lifecycle is tied to {@code this} component.
     * <p>
     * Use the returned {@link ShortcutRegistration} to fluently configure the
     * shortcut.
     *
     * @param key
     *            primary {@link Key} used to trigger the shortcut. Cannot be
     *            null.
     * @param keyModifiers
     *            {@link KeyModifier KeyModifiers} that need to be pressed along
     *            with the {@code key} for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring the shortcut and
     *         removing
     */
    default ShortcutRegistration addFocusShortcut(Key key,
            KeyModifier... keyModifiers) {
        if (!(this instanceof Component)) {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addFocusShortcut(Key, KeyModifier...)"));
        }

        if (key == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "key"));
        }

        final Component thisComponent = (Component) this;

        return new ShortcutRegistration((Component) this,
                () -> new Component[] { thisComponent.getUI().get() },
                event -> this.focus(), key).withModifiers(keyModifiers);
    }
}
