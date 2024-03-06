/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
