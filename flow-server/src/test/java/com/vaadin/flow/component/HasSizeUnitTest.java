/*
 * Copyright 2000-2025 Vaadin Ltd.
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
