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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.EventObject;

/**
 * An event fired when the value of a property changes.
 *
 * @since 1.0
 */
public class PropertyChangeEvent extends EventObject {

    private final boolean userOriginated;

    private final String propertyName;

    private final Serializable oldValue;
    private final Serializable value;

    /**
     * Creates a new {@code PropertyChangeEvent} event containing the current
     * property value of the given element.
     *
     * @param element
     *            the source element owning the property, not null
     * @param propertyName
     *            the property name
     * @param oldValue
     *            the previous value held by the source of this event
     * @param userOriginated
     *            {@code true} if this event originates from the client,
     *            {@code false} otherwise.
     */
    public PropertyChangeEvent(Element element, String propertyName,
            Serializable oldValue, boolean userOriginated) {
        super(element);
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.value = element.getPropertyRaw(propertyName);
        this.userOriginated = userOriginated;
    }

    /**
     * Returns the value of the source before this value change event occurred.
     * 
     * @return the value previously held by the source of this event
     */
    public Serializable getOldValue() {
        return oldValue;
    }

    /**
     * Returns the new value that triggered this value change event.
     *
     * @return the new value
     */
    public Serializable getValue() {
        return value;
    }

    /**
     * Returns whether this event was triggered by user interaction, on the
     * client side, or programmatically, on the server side.
     *
     * @return {@code true} if this event originates from the client,
     *         {@code false} otherwise.
     */
    public boolean isUserOriginated() {
        return userOriginated;
    }

    /**
     * Returns the property name.
     * 
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }
}
