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

package com.vaadin.flow.component.page;

import com.vaadin.flow.component.UI;

/**
 * Constains all possible attributes for {@link Viewport}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum ViewportAttribute {

    /**
     * The width of the virtual viewport of the device.
     */
    WIDTH("width"),

    /**
     * The physical width of the device's screen.
     */
    DEVICE_WIDTH("device-width"),

    /**
     * The height of the "virtual viewport" of the device.
     */
    HEIGHT("height"),

    /**
     * The physical height of the device's screen.
     */
    DEVICE_HEIGHT("device-height"),

    /**
     * The initial zoom when visiting the page. 1.0 does not zoom.
     */
    INITIAL_SCALE("initial-scale"),

    /**
     * The minimum amount the visitor can zoom on the page. 1.0 does not zoom.
     */
    MINIMUM_SCALE("minimum-scale"),

    /**
     * The maximum amount the visitor can zoom on the page. 1.0 does not zoom.
     */
    MAXIMUM_SCALE("maximum-scale"),

    /**
     * Allows the device to zoom in and out. Values are yes or no.
     */
    USER_SCALABLE("user-scalable");

    private final String attribute;

    ViewportAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public String toString() {
        return attribute;
    }
}
