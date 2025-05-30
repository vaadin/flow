package com.vaadin.flow.component.html;

import org.junit.Test;

public class CodeTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    @Override
    public void testHasAriaLabelIsNotImplemented() {
        // Don't use aria-label or aria-labelledby on any other non-interactive
        // content such as p, legend, li, or ul, because it is ignored.
        // Source: https://www.w3.org/TR/using-aria/#label-support
        super.testHasAriaLabelIsNotImplemented();
    }
}
