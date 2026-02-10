/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.Constants;

class HasSizeTest {

    @Tag("div")
    public static class HasSizeComponent extends Component implements HasSize {

    }

    @Test
    public void setWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        Assertions.assertEquals("100px", c.getWidth());
    }

    @Test
    public void setMinWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinWidth("100px");
        Assertions.assertEquals("100px", c.getMinWidth());
    }

    @Test
    public void setMaxWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxWidth("100px");
        Assertions.assertEquals("100px", c.getMaxWidth());
    }

    @Test
    public void removeWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        Assertions.assertEquals("100px", c.getWidth());

        c.setWidth(null);
        Assertions.assertNull(c.getWidth());
    }

    @Test
    public void removeMinWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinWidth("100px");
        Assertions.assertEquals("100px", c.getMinWidth());

        c.setMinWidth(null);
        Assertions.assertNull(c.getMinWidth());
    }

    @Test
    public void removeMaxWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxWidth("100px");
        Assertions.assertEquals("100px", c.getMaxWidth());

        c.setMaxWidth(null);
        Assertions.assertNull(c.getMaxWidth());
    }

    @Test
    public void setHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assertions.assertEquals("100px", c.getHeight());
    }

    @Test
    public void setMinHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinHeight("100px");
        Assertions.assertEquals("100px", c.getMinHeight());
    }

    @Test
    public void setMaxHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxHeight("100px");
        Assertions.assertEquals("100px", c.getMaxHeight());
    }

    @Test
    public void removeHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assertions.assertEquals("100px", c.getHeight());

        c.setHeight(null);
        Assertions.assertNull(c.getHeight());
    }

    @Test
    public void removeMinHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMinHeight("100px");
        Assertions.assertEquals("100px", c.getMinHeight());

        c.setMinHeight(null);
        Assertions.assertNull(c.getMinHeight());
    }

    @Test
    public void removeMaxHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setMaxHeight("100px");
        Assertions.assertEquals("100px", c.getMaxHeight());

        c.setMaxHeight(null);
        Assertions.assertNull(c.getMaxHeight());
    }

    @Test
    public void setSizeFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();

        Assertions.assertEquals("100%", component.getWidth());
        Assertions.assertEquals("100%", component.getHeight());
    }

    @Test
    public void setSizeFull_addsDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();

        Assertions.assertTrue(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertTrue(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setSizeFull_setSize_removesDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();

        component.setWidth("10px");
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertTrue(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));

        component.setHeight("10px");
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setSizeFull_setSizeUndefined_removesDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();
        component.setSizeUndefined();

        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setWidthFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidthFull();

        Assertions.assertEquals("100%", component.getWidth());
    }

    @Test
    public void setWidthFull_addsDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidthFull();

        Assertions.assertTrue(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setWidthFull_setWidth_removesDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidthFull();
        component.setWidth("10px");

        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setHeightFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setHeightFull();

        Assertions.assertEquals("100%", component.getHeight());
    }

    @Test
    public void setHeightFull_addsDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setHeightFull();

        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertTrue(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setHeightFull_setHeight_removesDataAttribute() {
        HasSizeComponent component = new HasSizeComponent();
        component.setHeightFull();
        component.setHeight("10px");

        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
        Assertions.assertFalse(component.getElement()
                .hasAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void setSizeUndefined() {
        HasSizeComponent component = new HasSizeComponent();
        component.setWidth("10px");
        component.setHeight("5em");

        component.setSizeUndefined();

        Assertions.assertNull(component.getWidth());
        Assertions.assertNull(component.getHeight());
    }

    @Test
    public void getWidthUnit() {
        HasSizeComponent component = new HasSizeComponent();
        Assertions.assertFalse(component.getWidthUnit().isPresent());

        component.setWidth("10px");
        Assertions.assertTrue(component.getWidthUnit().isPresent());
        Assertions.assertEquals(Unit.PIXELS, component.getWidthUnit().get());

        component.setSizeUndefined();
        Assertions.assertFalse(component.getWidthUnit().isPresent());
    }

    @Test
    public void getHeightUnit() {
        HasSizeComponent component = new HasSizeComponent();
        Assertions.assertFalse(component.getHeightUnit().isPresent());

        component.setHeight("10%");
        Assertions.assertTrue(component.getHeightUnit().isPresent());
        Assertions.assertEquals(Unit.PERCENTAGE,
                component.getHeightUnit().get());

        component.setSizeUndefined();
        Assertions.assertFalse(component.getHeightUnit().isPresent());
    }
}
