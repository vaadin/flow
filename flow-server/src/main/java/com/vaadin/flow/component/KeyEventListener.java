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

import java.util.Arrays;
import java.util.EnumSet;

import com.vaadin.flow.component.internal.KeyboardEvent;

/**
 * A conditional event listener for {@link KeyboardEvent}s.
 *
 * @param <E>
 *            the type of the {@link KeyboardEvent}
 * @since 1.0
 */
public class KeyEventListener<E extends KeyboardEvent>
        implements ComponentEventListener<E> {

    private final ComponentEventListener<E> listener;

    private final Key key;

    private final EnumSet<KeyModifier> modifiers;

    /**
     * Create a listener which will delegate to {@code listener} only if
     * {@code key} is the target key. If any {@code modifiers} is required, the
     * delegation occurs only if all the modifiers keys where pressed.
     *
     * @param listener
     *            the listener to delegate
     * @param key
     *            the key to check
     * @param modifiers
     *            the optional modifier keys
     */
    public KeyEventListener(ComponentEventListener<E> listener, Key key,
            KeyModifier... modifiers) {
        this.listener = listener;
        this.key = key;
        if (modifiers.length > 0) {
            this.modifiers = EnumSet.of(modifiers[0],
                    Arrays.copyOfRange(modifiers, 1, modifiers.length));
        } else {
            this.modifiers = EnumSet.noneOf(KeyModifier.class);
        }
    }

    @Override
    public void onComponentEvent(E event) {
        if (key.getKeys().stream().anyMatch(event.getKey()::matches)
                && event.getModifiers().containsAll(modifiers)) {
            listener.onComponentEvent(event);
        }
    }

}
