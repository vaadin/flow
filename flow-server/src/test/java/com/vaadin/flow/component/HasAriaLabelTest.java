/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class HasAriaLabelTest {

    @Tag(Tag.MAIN) // main is used, because div is not a valid target by default
    private static class TestComponent extends Component
            implements HasAriaLabel {

    }

    @Test
    public void withoutAriaLabelComponent_getAriaLabelReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void withNullAriaLabel_getAriaLabelReturnsEmptyOptional() {
        TestComponent component = new TestComponent();
        component.setAriaLabel(null);
        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void withEmptyAriaLabel_getAriaLabelReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("");
        assertEquals("", component.getAriaLabel().get());
    }

    @Test
    public void withAriaLabel_setAriaLabelToNullClearsAriaLabel() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("test AriaLabel");

        component.setAriaLabel(null);
        assertFalse(component.getAriaLabel().isPresent());
    }

    @Test
    public void setAriaLabel() {
        TestComponent component = new TestComponent();
        component.setAriaLabel("test AriaLabel");

        assertEquals("test AriaLabel", component.getAriaLabel().get());
    }

    @Test
    public void withoutAriaLabelledByComponent_getAriaLabelledByReturnsEmptyOptional() {
        TestComponent component = new TestComponent();

        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void withNullAriaLabelledBy_getAriaLabelledByReturnsEmptyOptional() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy(null);
        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void withEmptyAriaLabelledBy_getAriaLabelledByReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("");
        assertEquals("", component.getAriaLabelledBy().get());
    }

    @Test
    public void withAriaLabelledBy_setAriaLabelledByToNullClearsAriaLabelledBy() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("test AriaLabelledBy");

        component.setAriaLabelledBy(null);
        assertFalse(component.getAriaLabelledBy().isPresent());
    }

    @Test
    public void setAriaLabelledBy() {
        TestComponent component = new TestComponent();
        component.setAriaLabelledBy("test AriaLabelledBy");

        assertEquals("test AriaLabelledBy",
                component.getAriaLabelledBy().get());
    }
}
