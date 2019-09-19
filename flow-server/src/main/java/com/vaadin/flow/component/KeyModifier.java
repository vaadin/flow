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
