/*
 * Copyright (C) 2000-2026 Vaadin Ltd
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
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    @Override
    public void testHasOrderedComponents() {
        // According to this article:
        // https://www.scottohara.me/blog/2021/07/16/section.html
        // aria-label and aria-labelled-by are the best way to provide an
        // accessible name
        super.testHasOrderedComponents();
    }

}
