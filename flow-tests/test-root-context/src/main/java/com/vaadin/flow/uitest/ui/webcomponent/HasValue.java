/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;

/**
 * Defines setters and getters for a "value" property with a default of 0.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasValue extends HasElement {

    class Descriptors {
        // Hides descriptors
        private static final PropertyDescriptor<Integer, Integer> valueProperty = PropertyDescriptors
                .propertyWithDefault("value", 0);
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    default int getValue() {
        return Descriptors.valueProperty.get(this);
    }

    /**
     * Sets the value.
     *
     * @param value
     *            the value to set
     */
    default void setValue(int value) {
        Descriptors.valueProperty.set(this, value);
    }
}
