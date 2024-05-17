/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NativeTableTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        whitelistProperty("captionText");
        super.setup();
    }

    @Test
    public void getCaption() {
        var component = (NativeTable) getComponent();
        NativeTableCaption caption = component.getCaption();
        AssertUtils.assertEquals("Caption does not match",
                component.getChildren().toList().get(0), caption);
    }

    @Test
    public void addsCaptionAsFirstChild() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        component.getHead();
        component.addBody();
        component.getFoot();
        var caption = component.getCaption();
        assertEquals(4, component.getChildren().count());
        AssertUtils.assertEquals("Caption is not the first child", caption,
                component.getChildren().findFirst().orElseThrow());
        AssertUtils.assertEquals("Table is not the caption's father",
                caption.getParent().orElseThrow(), component);

    }

    @Test
    public void setCaptionText() {
        var component = (NativeTable) getComponent();
        String expectedText = "Test caption text.";
        component.setCaptionText(expectedText);
        var caption = component.getCaption();
        assertEquals(expectedText, caption.getText());
    }

    @Test
    public void getCaptionText() {
        var component = (NativeTable) getComponent();
        String expectedText = "Test caption text.";
        var caption = component.getCaption();
        caption.setText(expectedText);
        assertEquals(expectedText, component.getCaptionText());
    }

    @Test
    public void removeCaption() {
        var component = (NativeTable) getComponent();
        var caption = component.getCaption();
        component.removeCaption();
        assertTrue(caption.getParent().isEmpty());
    }

    @Test
    public void getHead() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        NativeTableHeader head = component.getHead();
        AssertUtils.assertEquals("head was not added", component,
                head.getParent().orElseThrow());
    }

    @Test
    public void addHeadAfterCaption() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        var head = component.getHead();
        assertEquals(2, component.getChildren().count());
        int headIndex = component.getChildren().toList().indexOf(head);
        assertEquals(1, headIndex);
    }

    @Test
    public void removeHead() {
        var component = (NativeTable) getComponent();
        NativeTableHeader head = component.getHead();
        component.removeHead();
        assertTrue(head.getParent().isEmpty());
    }

    @Test
    public void getFoot() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        NativeTableFooter footer = component.getFoot();
        AssertUtils.assertEquals("footer was not added", component,
                footer.getParent().orElseThrow());
    }

    @Test
    public void removeFoot() {
        var component = (NativeTable) getComponent();
        NativeTableFooter footer = component.getFoot();
        component.removeFoot();
        assertTrue(footer.getParent().isEmpty());
    }

}
