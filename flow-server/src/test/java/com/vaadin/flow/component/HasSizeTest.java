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

public class HasSizeTest {

    @Tag("div")
    public static class HasSizeComponent extends Component implements HasSize {

    }

    @Test
    public void setWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        Assert.assertEquals("100px", c.getWidth());
    }

    @Test
    public void setMinWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinWidth("100px");
        Assert.assertEquals("100px", c.getMinWidth());
    }

    @Test
    public void setMaxWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxWidth("100px");
        Assert.assertEquals("100px", c.getMaxWidth());
    }

    @Test
    public void removeWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        Assert.assertEquals("100px", c.getWidth());

        c.setWidth(null);
        Assert.assertNull(c.getWidth());
    }

    @Test
    public void removeMinWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinWidth("100px");
        Assert.assertEquals("100px", c.getMinWidth());

        c.setMinWidth(null);
        Assert.assertNull(c.getMinWidth());
    }

    @Test
    public void removeMaxWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxWidth("100px");
        Assert.assertEquals("100px", c.getMaxWidth());

        c.setMaxWidth(null);
        Assert.assertNull(c.getMaxWidth());
    }

    @Test
    public void setHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assert.assertEquals("100px", c.getHeight());
    }

    @Test
    public void setMinHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinHeight("100px");
        Assert.assertEquals("100px", c.getMinHeight());
    }

    @Test
    public void setMaxHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxHeight("100px");
        Assert.assertEquals("100px", c.getMaxHeight());
    }

    @Test
    public void removeHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assert.assertEquals("100px", c.getHeight());

        c.setHeight(null);
        Assert.assertNull(c.getHeight());
    }

    @Test
    public void removeMinHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinHeight("100px");
        Assert.assertEquals("100px", c.getMinHeight());

        c.setMinHeight(null);
        Assert.assertNull(c.getMinHeight());
    }

    @Test
    public void removeMaxHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxHeight("100px");
        Assert.assertEquals("100px", c.getMaxHeight());

        c.setMaxHeight(null);
        Assert.assertNull(c.getMaxHeight());
    }

    @Test
    public void setSizeFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();

        Assert.assertEquals("100%", component.getWidth());
        Assert.assertEquals("100%", component.getHeight());
    }

    @Test
    public void setWidthFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidthFull();

        Assert.assertEquals("100%", component.getWidth());
    }

    @Test
    public void setHeightFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setHeightFull();

        Assert.assertEquals("100%", component.getHeight());
    }

    @Test
    public void setSizeUndefined() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidth("10px");
        component.setHeight("5em");

        component.setSizeUndefined();

        Assert.assertNull(component.getWidth());
        Assert.assertNull(component.getHeight());
    }

    @Test
    public void getWidthUnit() {
        HasSizeComponent component = new HasSizeComponent();
        Assert.assertFalse(component.getWidthUnit().isPresent());

        component.setWidth("10px");
        Assert.assertTrue(component.getWidthUnit().isPresent());
        Assert.assertEquals(Unit.PIXELS, component.getWidthUnit().get());

        component.setSizeUndefined();
        Assert.assertFalse(component.getWidthUnit().isPresent());
    }

    @Test
    public void getHeightUnit() {
        HasSizeComponent component = new HasSizeComponent();
        Assert.assertFalse(component.getHeightUnit().isPresent());

        component.setHeight("10%");
        Assert.assertTrue(component.getHeightUnit().isPresent());
        Assert.assertEquals(Unit.PERCENTAGE, component.getHeightUnit().get());

        component.setSizeUndefined();
        Assert.assertFalse(component.getHeightUnit().isPresent());
    }
}
