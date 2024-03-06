/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
