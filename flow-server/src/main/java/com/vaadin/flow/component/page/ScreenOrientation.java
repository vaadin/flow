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
package com.vaadin.flow.component.page;

/**
 * Represents the orientation types reported by the Screen Orientation API.
 * <p>
 * These correspond to the concrete orientation types that can be observed as
 * the current screen orientation state.
 *
 * @author Vaadin Ltd
 */
public enum ScreenOrientation {

    PORTRAIT_PRIMARY("portrait-primary"),
    PORTRAIT_SECONDARY("portrait-secondary"),
    LANDSCAPE_PRIMARY("landscape-primary"),
    LANDSCAPE_SECONDARY("landscape-secondary");

    private final String clientValue;

    ScreenOrientation(String clientValue) {
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
     * Converts a client-side orientation type string to the corresponding enum
     * value.
     *
     * @param clientValue
     *            the orientation type string from the browser
     * @return the corresponding enum value
     * @throws IllegalArgumentException
     *             if the value does not match any known orientation type
     */
    public static ScreenOrientation fromClientValue(String clientValue) {
        for (ScreenOrientation orientation : values()) {
            if (orientation.clientValue.equals(clientValue)) {
                return orientation;
            }
        }
        throw new IllegalArgumentException(
                "Unknown screen orientation type: " + clientValue);
    }
}
