/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HasLabelTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component implements HasLabel {

    }

    @Test
    public void withoutLabelComponent_getLabelReturnsNull() {
        TestComponent component = new TestComponent();

        assertNull(component.getLabel());
    }

    @Test
    public void withNullLabel_getLabelReturnsNull() {
        TestComponent component = new TestComponent();
        component.setLabel(null);
        assertNull(component.getLabel());
    }

    @Test
    public void withEmptyLabel_getLabelReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setLabel("");
        assertEquals("", component.getLabel());
    }

    @Test
    public void setLabel() {
        TestComponent component = new TestComponent();
        component.setLabel("test label");

        assertEquals("test label", component.getLabel());
    }

}
