/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;

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
    public void removeWidth() {
        HasSizeComponent c = new HasSizeComponent();
        c.setWidth("100px");
        c.setWidth(null);
        Assert.assertNull(c.getWidth());
    }

    @Test
    public void setHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        Assert.assertEquals("100px", c.getHeight());
    }

    @Test
    public void removeHeight() {
        HasSizeComponent c = new HasSizeComponent();
        c.setHeight("100px");
        c.setHeight(null);
        Assert.assertNull(c.getHeight());
    }

    @Test
    public void setSizeFull() {
        HasSizeComponent component = new HasSizeComponent();
        component.setSizeFull();

        Assert.assertEquals("100%", component.getWidth());
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

}
