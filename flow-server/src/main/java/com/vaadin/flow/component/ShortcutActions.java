/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.vaadin.flow.component;

import java.security.InvalidParameterException;

import com.vaadin.flow.function.SerializableRunnable;

/**
 * @author Vaadin Ltd.
 * @since
 */
public final class ShortcutActions {
    private static final String NULL_PARAM = "Parameter %s must not be null!";

    /**
     * Adds a focus shortcut to a {@link Focusable} {@link Component}. The
     * <code>Focusable</code> interface provides a shortcut for this method.
     *
     * @param focusable Focusable component
     * @return {@link ShortcutRegistration} for configuring the shortcut
     * @see Focusable#addFocusShortcut()
     */
    public static ShortcutRegistration focus(Focusable focusable) {
        if (focusable == null) {
            throw new InvalidParameterException(
                    String.format(NULL_PARAM, "focusable"));
        }
        if (!(focusable instanceof Component)) {
            throw new InvalidParameterException(String.format(
                    "Parameter focusable must extend %s otherwise it is not " +
                            "qualified for a shortcut.",
                    Component.class.getSimpleName()));
        }

        return new ShortcutRegistration(focusable::focus,(Component) focusable);
    }

    /**
     * Adds a click shortcut to a {@link Component} which implements the
     * {@link ClickNotifier} interface. The <code>ClickNotifier</code> interface
     * offers a shorthand for this method.
     *
     * @param clickNotifier ClickNotifier component
     * @return {@link ShortcutRegistration} for configuring the shortcut
     * @see ClickNotifier#addClickShortcut()
     */
    public static ShortcutRegistration click(ClickNotifier clickNotifier) {
        if (clickNotifier == null) {
            throw new InvalidParameterException(
                    String.format(NULL_PARAM, "clickNotifier"));
        }
        if (!(clickNotifier instanceof Component)) {
            throw new InvalidParameterException(String.format(
                    "Parameter clickNotifier must extend %s otherwise it is " +
                            "not qualified for a shortcut.",
                    Component.class.getSimpleName()));
        }

        return new ShortcutRegistration(
                () -> ComponentUtil.fireEvent(
                        (Component)clickNotifier,
                        new ClickEvent<>((Component) clickNotifier)),
                (Component) clickNotifier);
    }

    /**
     * Invoke a {@link Runnable} as a result of a shortcut. Registering
     * a shortcut using this method will tie it to the current UI and the
     * shortcut is available in the global scope.
     * <p>
     *     In order to tie the shortcut to a different owner, use
     *     {@link ShortcutActions#exec(SerializableRunnable, Component)}
     *     instead. To limit the availability of the shortcut, use
     *     {@link ShortcutRegistration#scope(Component, Component...)} to
     *     define the components inside which the shortcut should work.
     *
     * @param runnable  Code to execute when the shortcut is invoked
     * @return {@link ShortcutRegistration} for configuring the shortcut.
     */
    public static ShortcutRegistration exec(SerializableRunnable runnable) {
        if (runnable == null) {
            throw new InvalidParameterException(
                    String.format(NULL_PARAM, "runnable"));
        }
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException("Shortcut cannot be registered " +
                    "without lifecycle owner when UI is not available! " +
                    "Register global shortcut only when UI is available.");
        }

        return exec(runnable, ui);
    }

    /**
     * Ties a {@link Runnable} to a shortcut owner by {@link Component owner}.
     *
     * @param runnable  Code to execute when the shortcut is invoked
     * @param owner     Component which controls whether the shortcut is
     *                  available or not
     * @return {@link ShortcutRegistration} for configuring the shortcut.
     * @see ShortcutActions#exec(SerializableRunnable)
     */
    public static ShortcutRegistration exec(SerializableRunnable runnable,
                                            Component owner) {
        if (runnable == null) {
            throw new InvalidParameterException(
                    String.format(NULL_PARAM, "runnable"));
        }
        return new ShortcutRegistration(runnable, owner);
    }
}
