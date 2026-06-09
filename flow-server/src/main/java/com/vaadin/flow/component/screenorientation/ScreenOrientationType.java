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
package com.vaadin.flow.component.screenorientation;

/**
 * Represents the orientation type of the browser screen.
 * <p>
 * Mirrors the values reported by the browser's <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/API/Screen_Orientation_API">Screen
 * Orientation API</a>, plus an {@link #UNKNOWN} sentinel used before the first
 * value has arrived from the client and an {@link #UNSUPPORTED} sentinel for
 * browsers that do not implement the API.
 *
 * @see ScreenOrientation#orientationSignal()
 */
public enum ScreenOrientationType {

    /**
     * No value has been reported by the browser yet. Used as the initial value
     * of the signal before the first client handshake delivers a real one. In
     * normal request handling the signal is seeded before any user code runs,
     * so this value is essentially never observed in practice; once a real
     * value has arrived, the signal never returns to {@code UNKNOWN}.
     */
    UNKNOWN(""),

    /**
     * The browser does not implement the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/Screen_Orientation_API">Screen
     * Orientation API</a>. Distinct from {@link #UNKNOWN} so callers can tell
     * "no data yet" apart from "the platform will never produce data."
     */
    UNSUPPORTED("unsupported"),

    /**
     * The screen is in primary portrait orientation (the device is held upright
     * in its natural portrait position).
     */
    PORTRAIT_PRIMARY("portrait-primary"),

    /**
     * The screen is in secondary portrait orientation (the device is rotated
     * 180° from {@link #PORTRAIT_PRIMARY}).
     */
    PORTRAIT_SECONDARY("portrait-secondary"),

    /**
     * The screen is in primary landscape orientation (the device is rotated 90°
     * clockwise from its natural portrait position).
     */
    LANDSCAPE_PRIMARY("landscape-primary"),

    /**
     * The screen is in secondary landscape orientation (the device is rotated
     * 90° counter-clockwise from its natural portrait position).
     */
    LANDSCAPE_SECONDARY("landscape-secondary");

    private final String clientValue;

    ScreenOrientationType(String clientValue) {
        this.clientValue = clientValue;
    }

    /**
     * Returns the value as used by the browser's Screen Orientation API.
     *
     * @return the client-side orientation type string
     */
    public String getClientValue() {
        return clientValue;
    }

    /**
     * Returns {@code true} for the two landscape orientations
     * ({@link #LANDSCAPE_PRIMARY}, {@link #LANDSCAPE_SECONDARY}).
     * {@link #UNKNOWN} and {@link #UNSUPPORTED} return {@code false}.
     *
     * @return whether this is a landscape orientation
     */
    public boolean isLandscape() {
        return this == LANDSCAPE_PRIMARY || this == LANDSCAPE_SECONDARY;
    }

    /**
     * Returns {@code true} for the two portrait orientations
     * ({@link #PORTRAIT_PRIMARY}, {@link #PORTRAIT_SECONDARY}).
     * {@link #UNKNOWN} and {@link #UNSUPPORTED} return {@code false}.
     *
     * @return whether this is a portrait orientation
     */
    public boolean isPortrait() {
        return this == PORTRAIT_PRIMARY || this == PORTRAIT_SECONDARY;
    }

    /**
     * Returns the enum constant matching the given client-side orientation type
     * string.
     *
     * @param clientValue
     *            the orientation type string from the browser
     * @return the corresponding enum value
     * @throws IllegalArgumentException
     *             if the value does not match any known orientation type
     */
    public static ScreenOrientationType fromClientValue(String clientValue) {
        for (ScreenOrientationType orientation : values()) {
            if (orientation.clientValue.equals(clientValue)) {
                return orientation;
            }
        }
        throw new IllegalArgumentException(
                "Unknown screen orientation type: " + clientValue);
    }
}
