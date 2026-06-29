/*
 * Copyright 2000-2026 Vaadin Ltd.
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

/**
 * Virtual keyboard hints for the {@code inputmode} attribute. They tell the
 * browser which type of virtual keyboard to display when the user interacts
 * with the field on a mobile device.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode
 * @since 25.3
 */
public enum InputMode {

    /**
     * No virtual keyboard. Useful when the field implements its own keyboard
     * input control.
     */
    NONE("none"),

    /**
     * Standard text input keyboard for the user's current locale.
     */
    TEXT("text"),

    /**
     * Fractional numeric input keyboard that includes the digits and the
     * decimal separator for the user's locale.
     */
    DECIMAL("decimal"),

    /**
     * Numeric input keyboard that only requires the digits 0&ndash;9.
     */
    NUMERIC("numeric"),

    /**
     * Telephone keypad input, including the digits 0&ndash;9, the asterisk (*),
     * and the pound (#) key.
     */
    TEL("tel"),

    /**
     * Virtual keyboard optimized for search input.
     */
    SEARCH("search"),

    /**
     * Virtual keyboard optimized for entering email addresses.
     */
    EMAIL("email"),

    /**
     * Virtual keyboard optimized for entering URLs.
     */
    URL("url");

    private final String value;

    InputMode(String value) {
        this.value = value;
    }

    /**
     * Gets the {@code inputmode} attribute value.
     *
     * @return the {@code inputmode} attribute value
     */
    public String getValue() {
        return value;
    }
}
