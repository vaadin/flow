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

public class SectionTest extends ComponentTest {
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
        // For most assistive technology it's fine to use aria-label or
        // aria-labelledby on the <nav>, and <main> elements but not
        // on <footer>, <section>, <article>, or <header>.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }
}
