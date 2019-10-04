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

import java.io.Serializable;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event when shortcut is detected.
 *
 * @author  Vaadin Ltd.
 * @since 1.3
 */
public class ShortcutEvent extends EventObject implements Serializable {
    private Component lifecycleOwner;
    private Key key;
    private Set<KeyModifier> keyModifiers;

    /**
     * Creates a new {@code ShortcutEvent}.
     *
     * @param source
     *          shortcut's {@code listenOn} {@link Component}
     * @param lifecycleOwner
     *          shortcut's {@code lifecycleOwner} {@link Component}
     * @param key
     *          primary {@link Key} of the shortcut
     * @param keyModifiers
     *          set of {@link KeyModifier KeyModifiers} of the shortcut
     */
    public ShortcutEvent(Component source, Component lifecycleOwner, Key key,
                         Set<KeyModifier> keyModifiers) {
        super(source);
        this.lifecycleOwner = lifecycleOwner;
        this.key = key;
        this.keyModifiers = keyModifiers == null ? Collections.emptySet() :
                Collections.unmodifiableSet(keyModifiers);
    }

    /**
     * Component which listened for the shortcut.
     * @return
     *          listening {@link Component}
     */
    @Override
    public Component getSource() {
        return (Component) super.getSource();
    }

    /**
     * Component which owns the shortcut.
     * @return
     *          owning {@link Component}
     */
    public Component getLifecycleOwner() {
        return lifecycleOwner;
    }

    /**
     * Primary {@link Key} that triggered the shortcut. Primary key can be
     * anything that is not a {@link KeyModifier}.
     * @return
     *          primary key
     */
    public Key getKey() {
        return key;
    }

    /**
     * Set of {@link KeyModifier KeyModifiers} that, in combination with the
     * primary key, triggered the shortcut.
     * @return
     *          set of key modifiers
     */
    public Set<KeyModifier> getKeyModifiers() {
        return keyModifiers;
    }

    /**
     * Checks if the event matches the given {@link Key} and (optional)
     * {@link KeyModifier KeyModifiers}. If {@code key} is null or a wrong
     * number of {@code keyModifiers} is given, returns {@code false}.
     *
     * @param key           {@code key} to compare
     * @param keyModifiers  {@code keyModifiers} to compare
     * @return Did the given parameters match those in the event?
     */
    public boolean matches(Key key, KeyModifier... keyModifiers) {
        if (key == null) {
            return false;
        }
        if (keyModifiers.length != this.keyModifiers.size()) {
            return false;
        }
        List<String> keyStrings = Stream.of(keyModifiers)
                .map(k -> k.getKeys().get(0)).collect(Collectors.toList());
        return key.matches(this.key.getKeys().get(0)) && this.keyModifiers
                .stream().allMatch(k -> keyStrings.stream()
                        .anyMatch(k::matches));
    }
}
