/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Test;

public class DivTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    @Override
    public void testHasOrderedComponents() {
        super.testHasOrderedComponents();
    }

    @Test
    @Override
    public void testHasAriaLabelIsNotImplemented() {
        // Don't use aria-label or aria-labelledby on a span or div unless
        // its given a role. When aria-label or aria-labelledby are on
        // interactive roles (such as a link or button) or an img role,
        // they override the contents of the div or span.
        // Other roles besides Landmarks (discussed above) are ignored.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }
}
