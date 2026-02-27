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

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * Handle for a client-side click-to-copy registration created by
 * {@link Clipboard#copyOnClick(com.vaadin.flow.component.Component, String)}.
 * <p>
 * Allows updating the text that will be copied and removing the click handler
 * when no longer needed.
 */
public class ClipboardCopy implements Registration, Serializable {

    static final String CLIPBOARD_TEXT_PROPERTY = "__clipboardText";

    private final Element triggerElement;
    private final Registration cleanupRegistration;

    /**
     * Creates a new clipboard copy handle.
     *
     * @param triggerElement
     *            the element that has the click handler installed
     * @param cleanupRegistration
     *            the registration to call when removing the click handler
     */
    ClipboardCopy(Element triggerElement, Registration cleanupRegistration) {
        this.triggerElement = triggerElement;
        this.cleanupRegistration = cleanupRegistration;
    }

    /**
     * Updates the text that will be copied to the clipboard when the trigger
     * element is clicked.
     * <p>
     * The new value is pushed to the client-side so the copy operation can
     * execute without a server round-trip.
     *
     * @param text
     *            the new text to copy, or an empty string if {@code null}
     */
    public void setValue(String text) {
        triggerElement.setProperty(CLIPBOARD_TEXT_PROPERTY,
                text != null ? text : "");
    }

    /**
     * Removes the client-side click handler and cleans up associated resources.
     */
    @Override
    public void remove() {
        cleanupRegistration.remove();
    }
}
