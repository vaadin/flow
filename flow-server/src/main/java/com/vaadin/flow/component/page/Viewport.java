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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a viewport tag that will be added to the HTML of the host page of a
 * UI class. If no viewport tag has been defined, a default of
 * <code>width=device-width, initial-scale=1.0</code> is used.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Viewport {
    /**
     * Sets the viewport at the width of the device.
     */
    String DEFAULT = "width=device-width, initial-scale=1.0";

    /**
     * Sets the viewport at the height of the device.
     */
    String DEVICE_HEIGHT = "height=device-height, initial-scale=1.0";

    /**
     * Sets the viewport at the width and height of the device.
     */
    String DEVICE_DIMENSIONS = "width=device-width, height=device-height, initial-scale=1.0";

    /**
     * Sets the viewport at the width of the device and sets the minimum-scale
     * to 1.0.
     */
    String DEVICE_WIDTH_NO_ZOOM_OUT = "width=device-width, initial-scale=1.0, minimum-scale=1.0";

    /**
     * Sets the viewport at the width of the device and sets the maximum-scale
     * to 1.0.
     */
    String DEVICE_WIDTH_NO_ZOOM_IN = "width=device-width, initial-scale=1.0, maximum-scale=1.0";

    /**
     * Sets the viewport at the height of the device and sets the minimum-scale
     * to 1.0.
     */
    String DEVICE_HEIGHT_NO_ZOOM_OUT = "height=device-height, initial-scale=1.0, minimum-scale=1.0";

    /**
     * Sets the viewport at the height of the device and sets the maximum-scale
     * to 1.0.
     */
    String DEVICE_HEIGHT_NO_ZOOM_IN = "height=device-height, initial-scale=1.0, maximum-scale=1.0";

    /**
     * Sets the viewport at the width and height of the device and sets the
     * minimum-scale to 1.0.
     */
    String DEVICE_DIMENSIONS_NO_ZOOM_OUT = "width=device-width, height=device-height, initial-scale=1.0, minimum-scale=1.0";

    /**
     * Sets the viewport at the width and height of the device and sets the
     * maximum-scale to 1.0.
     */
    String DEVICE_DIMENSIONS_NO_ZOOM_IN = "width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0";

    /**
     * Prevents scaling and prevent the user from being able to zoom.
     */
    String NO_SCALABLE = "width=device-width, initial-scale=1.0, minimum-scale=1.0, user-scalable=no";

    /**
     * Gets the viewport tag content.
     *
     * @return the viewport tag content
     */
    String value();
}
