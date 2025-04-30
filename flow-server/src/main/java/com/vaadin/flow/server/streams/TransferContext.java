/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.streams;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * A context that is given to all data transfer progress listeners. Holds the
 * references that may be needed for UI updates in listeners, e.g. showing a
 * data transfer progress or a notification.
 *
 * @param request
 *            current Vaadin request instance
 * @param response
 *            current Vaadin response instance
 * @param session
 *            current Vaadin session instance
 * @param fileName
 *            name of the file being transferred
 * @param owningElement
 *            the element that initiated the transfer
 * @param totalBytes
 *            the total number of bytes to be transferred or <code>-1</code> if
 *            total number is unknown in advance, e.g. when reading from an
 *            input stream
 */
public record TransferContext(VaadinRequest request, VaadinResponse response,
        VaadinSession session, String fileName, Element owningElement,
        long totalBytes) {

    /**
     * Get owner {@link Component} for this event.
     *
     * @return owning component or null in none defined
     */
    public Component getOwningComponent() {
        return owningElement.getComponent().orElse(null);
    }

    /**
     * Get the UI instance for this request.
     *
     * @return Current UI
     */
    public UI getUI() {
        Optional<Component> component = owningElement.getComponent();
        return component.map(value -> value.getUI().orElseGet(UI::getCurrent))
                .orElseGet(UI::getCurrent);
    }
}
