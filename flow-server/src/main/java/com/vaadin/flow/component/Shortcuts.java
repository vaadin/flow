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

import com.vaadin.flow.server.Command;


/**
 * @author Vaadin Ltd.
 * @since
 */
public final class Shortcuts {
    static final String NULL = "Parameter '%s' must not be null!";

    /**
     * TODO
     * Invoke a {@link Runnable} as a result of a shortcut. Registering
     * a shortcut using this method will tie it to the current UI and the
     * shortcut is available in the global scope.
     * <p>
     *     In order to tie the shortcut to a different owner, use
     *     instead. To limit the availability of the shortcut, use
     *     define the components inside which the shortcut should work.
     *
     * @param owner
     * @param command  Code to execute when the shortcut is invoked
     * @param key
     * @param keyModifiers
     * @return {@link ShortcutRegistration} for configuring the shortcut.
     */
    public static ShortcutRegistration addShortcut(
            Component owner, Command command, Key key,
            KeyModifier... keyModifiers) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }
        if (key == null) {
            throw new InvalidParameterException(String.format(NULL, "key"));
        }
        return registerShortcut(owner, command, key)
                .withModifiers(keyModifiers);
    }

    /**
     * TODO
     * Ties a {@link Runnable} to a shortcut owner by {@link Component owner}.
     *
     * @param owner     Component which controls whether the shortcut is
     *                  available or not
     * @param command   Code to execute when the shortcut is invoked
     * @param character
     * @param keyModifiers
     * @return {@link ShortcutRegistration} for configuring the shortcut.
     * @see Shortcuts#addShortcut(Component, Command, Key, KeyModifier...)
     */
    public static ShortcutRegistration addShortcut(
            Component owner, Command command, char character,
            KeyModifier... keyModifiers) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }
        return registerShortcut(owner, command, character)
                .withModifiers(keyModifiers);
    }

    /**
     * TODO
     * @param owner
     * @param command
     * @param key
     * @return
     */
    public static ShortcutRegistration registerShortcut(
            Component owner, Command command, Key key) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }
        if (key == null) {
            throw new InvalidParameterException(String.format(NULL, "key"));
        }

        UI ui = UI.getCurrent();

        if (ui == null) {
            throw new IllegalStateException("Cannot register a shortcut with " +
                    "lifecycle bound to the UI when UI is not available.");
        }

        return new ShortcutRegistration(ui, () -> owner, command, key);
    }

    /**
     * TODO
     * @param owner
     * @param command
     * @param character
     * @return
     */
    public static ShortcutRegistration registerShortcut(
            Component owner, Command command, char character) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }

        UI ui = UI.getCurrent();

        if (ui == null) {
            throw new IllegalStateException("Cannot register a shortcut with " +
                    "lifecycle bound to the UI when UI is not available.");
        }

        return new ShortcutRegistration(ui, () -> owner, command, character);
    }

    /**
     * TODO
     * @param owner
     * @param command
     * @param key
     * @return
     */
    public static ShortcutRegistration registerShortcut(
            Component owner, Component lifecycleOwner, Command command,
            Key key) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (lifecycleOwner == null) {
            throw new InvalidParameterException(String.format(NULL,
                    "lifecycleOwner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }
        if (key == null) {
            throw new InvalidParameterException(String.format(NULL, "key"));
        }

        return new ShortcutRegistration(lifecycleOwner, () -> owner, command,
                key);
    }

    /**
     * TODO
     * @param owner
     * @param command
     * @param character
     * @return
     */
    public static ShortcutRegistration registerShortcut(
            Component owner, Component lifecycleOwner, Command command,
            char character) {
        if (owner == null) {
            throw new InvalidParameterException(String.format(NULL, "owner"));
        }
        if (lifecycleOwner == null) {
            throw new InvalidParameterException(String.format(NULL,
                    "lifecycleOwner"));
        }
        if (command == null) {
            throw new InvalidParameterException(String.format(NULL, "command"));
        }

        return new ShortcutRegistration(lifecycleOwner, () -> owner, command,
                character);
    }
}
