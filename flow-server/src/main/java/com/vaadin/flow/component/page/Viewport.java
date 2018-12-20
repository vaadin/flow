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
     * Sets the viewport at the width of the device. It makes the page match the
     * screen’s width in device-independent pixels, allowing its content to
     * reflow to match different screen sizes.
     * <p>
     * Recommended for a Responsive Web Design.
     */
    String DEFAULT = "width=device-width, initial-scale=1.0";

    /**
     * Sets the viewport to the height of the device rather than the rendered
     * space.
     */
    String DEVICE_HEIGHT = "height=device-height, initial-scale=1.0";

    /**
     * Sets the viewport at the width and height of the device. The device-width
     * and device-height properties are translated to 100vw and 100vh
     * respectively.
     */
    String DEVICE_DIMENSIONS = "width=device-width, height=device-height, initial-scale=1.0";

    /**
     * Gets the viewport tag content.
     *
     * @return the viewport tag content
     */
    String value();
}
