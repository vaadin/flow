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
 * <p>
 * Note: Methods' names are mapped into viewport attributes.
 *       E.g. initialScale to initial-scale
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Viewport {
    String DEFAULT = "width=device-width, initial-scale=1.0";

    /**
     * Gets the viewport tag content.
     *
     * @return the viewport tag content
     */
    String value();

    /**
     * Gets the width of the virtual viewport of the device.
     *
     * @return width width of the viewport
     */
    String width() default "device-width";

    /**
     * Gets the physical width of the device's screen.
     *
     * @return deviceWidth physical width of the device's screen
     */
    String deviceWidth() default "";

    /**
     * Gets the height of the virtual viewport of the device.
     *
     * @return height height of the viewport
     */
    String height() default "";

    /**
     * Gets the physical height of the device's screen.
     *
     * @return deviceHeight physical height of the device's screen
     */
    String deviceHeight() default "";

    /**
     * Gets the initial zoom when visiting the page. 1.0 does not zoom
     *
     * @return initialScale initial zoom when visiting the page
     */
    String initialScale() default "1.0";

    /**
     * Gets the minimum amount the visitor can zoom on the page. 1.0 does not
     * zoom.
     *
     * @return minimumScale minimum zoom
     */
    String minimumScale() default "";

    /**
     * Gets the maximum amount the visitor can zoom on the page. 1.0 does not
     * zoom.
     *
     * @return maximumScale maximum zoom
     */
    String maximumScale() default "";

    /**
     * Allows the device to zoom in and out. Values are yes or no.
     *
     * @return <code>true</code> if scalable, <code>false</code> otherwise.
     */
    boolean userScalable() default true;
}
