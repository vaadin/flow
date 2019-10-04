/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.server.Command;


/**
 * Collections of methods for configuring more complex Shortcut interactions.
 * <p>
 * Unlike the shortcut methods offered by {@link Focusable} and {@link
 * ClickNotifier}, these methods allow for configuring the {@code
 * lifecycleOwner} directly, making it possible to added the shortcut onto any
 * component. The {@code lifecycleOwner} denotes the component to which the
 * shortcut is bound to. If the lifecycle owner is not attached, visible, or
 * enabled, the shortcut won't work, and vice-versa.
 *
 * @author Vaadin Ltd.
 * @since 1.3
 *
 * @see Focusable#addFocusShortcut(Key, KeyModifier...) for adding a shortcut
 *         for focusing the component
 * @see ClickNotifier#addClickShortcut(Key, KeyModifier...) for adding a
 *         shortcut which performs the click-action
 */
public final class Shortcuts {
    static final String NULL = "Parameter '%s' must not be null!";

    private Shortcuts() {
    }

    /**
     * Invoke a {@link Command} when the shortcut is invoked.
     * <p>
     * Registering a shortcut using this method will tie it to {@code
     * lifecycleOwner} and the shortcut is available in the global scope.
     * <p>
     * By default, the shortcut's listener is bound to {@link UI}. The listening
     * component can be changed by calling {@link ShortcutRegistration#listenOn(Component)}.
     *
     * @param lifecycleOwner
     *         the component that controls, when the shortcut is active. If the
     *         component is either invisible or detached, the shortcut won't
     *         work. Cannot be {@code null}
     * @param command
     *         code to execute when the shortcut is invoked. Cannot be {@code
     *         null}
     * @param key
     *         primary {@link Key} used to trigger the shortcut. Cannot be
     *         {@code null}
     * @param keyModifiers
     *         {@link KeyModifier KeyModifiers} which also need to be pressed
     *         for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring and removing the
     *         shortcut
     */
    public static ShortcutRegistration addShortcutListener(
            Component lifecycleOwner, Command command, Key key,
            KeyModifier... keyModifiers) {
        if (lifecycleOwner == null) {
            throw new IllegalArgumentException(String.format(NULL,
                    "lifecycleOwner"));
        }
        if (command == null) {
            throw new IllegalArgumentException(String.format(NULL, "command"));
        }
        if (key == null) {
            throw new IllegalArgumentException(String.format(NULL, "key"));
        }
        return new ShortcutRegistration(lifecycleOwner, UI::getCurrent,
                event -> command.execute(), key).withModifiers(keyModifiers);
    }

    /**
     * Invoke a {@link ShortcutEventListener} when the shortcut is invoked.
     * <p>
     * Registering a shortcut using this method will tie it to {@code
     * lifecycleOwner} and the shortcut is available in the global scope.
     * <p>
     * By default, the shortcut's listener is bound to {@link UI}. The listening
     * component can be changed by calling {@link ShortcutRegistration#listenOn(Component)}.
     *
     * @param lifecycleOwner
     *         the component that controls, when the shortcut is active. If the
     *         component is either invisible or detached, the shortcut won't
     *         work. Cannot be {@code null}
     * @param listener
     *         listener to execute when the shortcut is invoked. Receives a
     *         {@link ShortcutEvent}. Cannot be {@code null}
     * @param key
     *         primary {@link Key} used to trigger the shortcut. Cannot be
     *         {@code null}
     * @param keyModifiers
     *         {@link KeyModifier KeyModifiers} which also need to be pressed
     *         for the shortcut to trigger
     * @return {@link ShortcutRegistration} for configuring and removing the
     *         shortcut
     */
    public static ShortcutRegistration addShortcutListener(
            Component lifecycleOwner, ShortcutEventListener listener, Key key,
            KeyModifier... keyModifiers) {

        if (lifecycleOwner == null) {
            throw new IllegalArgumentException(String.format(NULL,
                    "lifecycleOwner"));
        }
        if (listener == null) {
            throw new IllegalArgumentException(String.format(NULL,
                    "listener"));
        }
        if (key == null) {
            throw new IllegalArgumentException(String.format(NULL, "key"));
        }
        return new ShortcutRegistration(lifecycleOwner, UI::getCurrent,
                listener, key).withModifiers(keyModifiers);
    }
}
