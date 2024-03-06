/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.List;
import java.util.stream.Stream;

/**
 * Modifier keys.
 *
 * @since 1.0
 */
public enum KeyModifier implements Key {

    /**
     * KeyModifier for "{@code Shift}" key.
     */
    SHIFT(Key.SHIFT),

    /**
     * KeyModifier for "{@code Control}" key.
     */
    CONTROL(Key.CONTROL),

    /**
     * KeyModifier for "{@code Alt}" key.
     */
    ALT(Key.ALT),

    /**
     * KeyModifier for "{@code Alt Graph}" key.
     */
    ALT_GRAPH(Key.ALT_GRAPH),

    /**
     * KeyModifier for "{@code Meta}" key.
     */
    META(Key.META);

    private final Key key;

    KeyModifier(Key key) {
        this.key = key;
    }

    /**
     * Gets the key value.
     *
     * @return the key value
     */
    @Override
    public List<String> getKeys() {
        return key.getKeys();
    }

    /**
     * Returns the {@code KeyModifier} for {@code key}.
     *
     * @param key
     *            the key value
     * @return the {@code KeyModifier}
     */
    public static KeyModifier of(String key) {
        return Stream.of(values()).filter(k -> k.matches(key)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
