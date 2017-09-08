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
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.ui.HasElement;
import com.vaadin.ui.PropertyDescriptor;
import com.vaadin.ui.PropertyDescriptors;

/**
 * Defines setters and getters for a "max" property with a default of 100.
 *
 * @author Vaadin Ltd
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
