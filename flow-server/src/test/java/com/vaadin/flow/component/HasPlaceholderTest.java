/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HasPlaceholderTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasPlaceholder {

    }

    @Test
    public void withoutPlaceholderComponent_getPlaceholderReturnsNull() {
        TestComponent component = new TestComponent();

        assertNull(component.getPlaceholder());
    }

    @Test
    public void withNullPlaceholder_getPlaceholderReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setPlaceholder(null);
        assertEquals("", component.getPlaceholder());
    }

    @Test
    public void withEmptyPlaceholder_getPlaceholderReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setPlaceholder("");
        assertEquals("", component.getPlaceholder());
    }

    @Test
    public void setPlaceholder() {
        TestComponent component = new TestComponent();
        component.setPlaceholder("test Placeholder");

        assertEquals("test Placeholder", component.getPlaceholder());
    }

}
