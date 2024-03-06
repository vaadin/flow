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
 * Defines setters and getters for a "max" property with a default of 100.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasMax extends HasElement {

    class Descriptors {
        // Hides descriptors
        static final PropertyDescriptor<Integer, Integer> maxProperty = PropertyDescriptors
                .propertyWithDefault("max", 100);
    }

    /**
     * Sets the max value.
     *
     * @param max
     *            the max value to use
     */
    default void setMax(int max) {
        Descriptors.maxProperty.set(this, max);
    }

    /**
     * Gets the max value.
     *
     * @return the max value
     */
    default int getMax() {
        return Descriptors.maxProperty.get(this);
    }

}
