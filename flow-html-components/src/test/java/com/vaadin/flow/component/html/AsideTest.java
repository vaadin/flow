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

public class AsideTest extends ComponentTest {
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
        // There is mixed support for aria-label or aria-labelledby on <aside>.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }
}
