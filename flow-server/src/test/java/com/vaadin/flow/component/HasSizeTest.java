/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasSize.Unit;
import com.vaadin.flow.component.Tag;

public class HasSizeTest {

    @Tag("div")
    public static class HasSizeComponent extends Component implements HasSize {

    }

    @Test
    public void setWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setWidth(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getWidth());
        }
    }

    @Test
    public void setWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        Assert.assertEquals("100px", c.getWidth());
    }

    @Test
    public void setMinWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMinWidth(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getMinWidth());
        }
    }

    @Test
    public void setMinWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinWidth("100px");
        Assert.assertEquals("100px", c.getMinWidth());
    }

    @Test
    public void setMaxWidthFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMaxWidth(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getMaxWidth());
        }
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
    public void setHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setHeight(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getHeight());
        }
    }

    @Test
    public void setHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assert.assertEquals("100px", c.getHeight());
    }

    @Test
    public void setMinHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMinHeight(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getMinHeight());
        }
    }

    @Test
    public void setMinHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinHeight("100px");
        Assert.assertEquals("100px", c.getMinHeight());
    }

    @Test
    public void setMaxHeightFloat() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            c.setMaxHeight(100,unit);
            Assert.assertEquals("100.0"+unit.toString(), c.getMaxHeight());
        }
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
    public void getUnit() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            String cssSize = 100f+unit.toString();
            Optional<Unit> theUnit = Unit.getUnit(cssSize);
            Assert.assertTrue(theUnit.isPresent());
            Assert.assertEquals(theUnit.get(), unit);
        }
    }

    @Test
    public void getSize() {
        HasSizeComponent c = new HasSizeComponent();
        for (Unit unit : Unit.values()) {
            String cssSize = 100f+unit.toString();
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

    @Test(expected = IllegalArgumentException.class)
    public void getUnitException() {
        String cssSize = "";
        Optional<Unit> size = Unit.getUnit(cssSize);        	
    }
}
