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
 * A component which supports an {@link InputMode}, hinting at the type of
 * virtual keyboard to display when the user interacts with the field on a
 * mobile device.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode">The
 *      inputmode global attribute (MDN)</a>
 * @since 25.3
 */
public interface HasInputMode extends HasElement {

    /**
     * Sets the {@link InputMode} that hints at the type of virtual keyboard to
     * display when the user interacts with the field on a mobile device. If not
     * set, the browser falls back to the default virtual keyboard for the
     * element.
     *
     * @param inputMode
     *            the {@code inputmode} value, or {@code null} to unset
     */
    default void setInputMode(InputMode inputMode) {
        if (inputMode == null) {
            getElement().removeAttribute("inputmode");
        } else {
            getElement().setAttribute("inputmode", inputMode.getValue());
        }
    }

    /**
     * Gets the {@link InputMode} of this component.
     *
     * @return the {@code inputmode} value, or {@code null} if not set
     * @see #setInputMode(InputMode)
     */
    default InputMode getInputMode() {
        String inputMode = getElement().getAttribute("inputmode");
        if (inputMode == null || inputMode.isEmpty()) {
            return null;
        }
        return InputMode.valueOf(inputMode.toUpperCase());
    }
}
