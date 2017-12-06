/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.event;

import java.util.stream.Stream;

/**
 * Enumeration of modifier keys.
 */
public enum KeyModifier {

    /**
     * KeyModifier for "{@code Shift}" key.
     */
    SHIFT(com.vaadin.ui.event.Key.SHIFT),

    /**
     * KeyModifier for "{@code Control}" key.
     */
    CONTROL(com.vaadin.ui.event.Key.CONTROL),

    /**
     * KeyModifier for "{@code Alt}" key.
     */
    ALT(com.vaadin.ui.event.Key.ALT),

    /**
     * KeyModifier for "{@code Meta}" key.
     */
    META(com.vaadin.ui.event.Key.META);;

    private final Key key;

    KeyModifier(Key key) {
        this.key = key;
    }

    /**
     * Gets the {@code Key} key value.
     *
     * @return the key value
     */
    public Key getKey() {
        return key;
    }

    /**
     * Returns the {@code KeyModifier} for {@code key}.
     *
     * @param key
     *            the key value
     * @return the {@code Key}
     */
    public static KeyModifier of(Key key) {
        return Stream.of(values()).filter(k -> k.key == key).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
