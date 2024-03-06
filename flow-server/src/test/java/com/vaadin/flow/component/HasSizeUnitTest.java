/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class HasSizeUnitTest {

    @Tag("div")
    public static class HasSizeComponent extends Component implements HasSize {

    }

    @Test
    public void setWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setWidth(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getWidth());
        }
    }

    @Test
    public void setMinWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMinWidth(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getMinWidth());
        }
    }

    @Test
    public void setMaxWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMaxWidth(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getMaxWidth());
        }
    }

    @Test
    public void setHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setHeight(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getHeight());
        }
    }

    @Test
    public void setMinHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMinHeight(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getMinHeight());
        }
    }

    @Test
    public void setMaxHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMaxHeight(100, unit);
            Assert.assertEquals("100.0" + unit.toString(), c.getMaxHeight());
        }
    }

    @Test
    public void getUnit() {
        for (Unit unit : Unit.values()) {
            String cssSize = 100f + unit.toString();
            Optional<Unit> theUnit = Unit.getUnit(cssSize);
            Assert.assertTrue(theUnit.isPresent());
            Assert.assertEquals(theUnit.get(), unit);
        }
    }

    @Test
    public void getSize() {
        for (Unit unit : Unit.values()) {
            String cssSize = 100f + unit.toString();
            float size = Unit.getSize(cssSize);
            Assert.assertEquals(100f, size, 0.01);
        }
        for (Unit unit : Unit.values()) {
            String cssSize = unit.toString();
            float size = Unit.getSize(cssSize);
            Assert.assertEquals(0f, size, 0.01);
        }
    }

    @Test(expected = NumberFormatException.class)
    public void getSizeException() {
        String cssSize = "10a0px";
        float size = Unit.getSize(cssSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSizeNoUnit() {
        String cssSize = "100";
        float size = Unit.getSize(cssSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSizeNoValidUnit() {
        String cssSize = "100p";
        float size = Unit.getSize(cssSize);
    }

}
