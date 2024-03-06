/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

public class HasHelperTest {

    @Tag("div")
    public static class HasHelperComponent extends Component
            implements HasHelper {
    }

    @Test
    public void getHelperText() {
        final HasHelperComponent c = new HasHelperComponent();
        Assert.assertNull(c.getHelperText());
    }

    @Test
    public void getHelperComponent() {
        final HasHelperComponent c = new HasHelperComponent();
        Assert.assertNull(c.getHelperComponent());
    }

    @Test
    public void setHelperText() {
        final HasHelperComponent c = new HasHelperComponent();
        c.setHelperText("helper");
        Assert.assertEquals("helper", c.getHelperText());
    }

    @Test
    public void setHelperComponent() {
        final HasHelperComponent c = new HasHelperComponent();
        final HasHelperComponent slotted = new HasHelperComponent();
        c.setHelperComponent(slotted);
        Assert.assertEquals(slotted, c.getHelperComponent());
    }

    @Test
    public void removeHelperText() {
        final HasHelperComponent c = new HasHelperComponent();
        c.setHelperText("helper");
        Assert.assertEquals("helper", c.getHelperText());

        c.setHelperText(null);
        Assert.assertNull(c.getHelperText());
    }

    @Test
    public void removeHelperComponent() {
        final HasHelperComponent c = new HasHelperComponent();
        final HasHelperComponent slotted = new HasHelperComponent();

        c.setHelperComponent(slotted);
        Assert.assertEquals(slotted, c.getHelperComponent());

        c.setHelperComponent(null);
        Assert.assertNull(c.getHelperComponent());
    }
}
