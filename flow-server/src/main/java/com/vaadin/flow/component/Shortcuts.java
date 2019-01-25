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
import com.vaadin.flow.shared.Registration;


/**
 * @author Vaadin Ltd.
 * @since
 */
public final class Shortcuts {
    static final String NULL = "Parameter '%s' must not be null!";

    /**
     * Invoke a {@link Command} when the shortcut is invoked.
     * <p>
     * Registering a shortcut using this method will tie it to
     * {@code lifecycleOwner} and the shortcut is available in the global scope.
     * <p>
     * By default, the shortcut's listener is bound to {@link UI}. The listening
     * component can be changed by calling
     * {@link ShortcutRegistration#listenOn(Component)}.
     *
     * @param lifecycleOwner
     *              The component that controls, when the shortcut is active. If
     *              the component is either invisible or detached, the shortcut
     *              won't work
     * @param command
     *              Code to execute when the shortcut is invoked
     * @param key
     *              Primary {@link Key} used to trigger the shortcut
     * @param keyModifiers
     *              {@link KeyModifier KeyModifiers} which also need to be
     *              pressed for the shortcut to trigger
     * @return
     *              {@link ShortcutRegistration} for configuring and removing the
     *              shortcut
     */
    public static ShortcutRegistration addShortcut(
            Component lifecycleOwner, Command command, Key key,
            KeyModifier... keyModifiers) {
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
        return new ShortcutRegistration(lifecycleOwner, UI::getCurrent,
                command, key).withModifiers(keyModifiers);
    }
}
