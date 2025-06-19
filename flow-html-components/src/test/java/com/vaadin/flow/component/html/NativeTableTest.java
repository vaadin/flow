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
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NativeTableTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
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

    @Test
    public void addBody() {
        var component = (NativeTable) getComponent();
        component.addBody();
        assertEquals(1, component.getChildren().count());
        component.addBody();
        assertEquals(2, component.getChildren().count());
    }

    @Test
    public void addBodyAfterCaption() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    public void addBodyAfterHeader() {
        var component = (NativeTable) getComponent();
        component.getHead();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    public void addBodyAfterBothCaptionAndHeader() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        component.getHead();
        var body = component.addBody();
        assertEquals(2, component.getChildren().toList().indexOf(body));
    }

    @Test
    public void getBody() {
        var component = (NativeTable) getComponent();
        var body = component.getBody();
        assertEquals(1, component.getChildren().count());
        // add a second body
        component.addBody();
        assertEquals(2, component.getChildren().count());
        // subsequent calls should return the same first body
        var secondCallBody = component.getBody();
        AssertUtils.assertEquals("No new body should've been created", body,
                secondCallBody);
    }

    @Test
    public void getBodyByIndex() {
        var component = (NativeTable) getComponent();
        var body = component.getBody(0);
        assertEquals(1, component.getChildren().count());
        var secondCallBody = component.getBody(0);
        assertEquals(1, component.getChildren().count());
        AssertUtils.assertEquals("No new body should've been created", body,
                secondCallBody);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNonExistentBodyByIndex() {
        var component = (NativeTable) getComponent();
        component.getBody(1);
    }

    @Test
    public void getBodies() {
        var component = (NativeTable) getComponent();
        for (int i = 0; i < 10; i++) {
            component.addBody();
        }
        List<NativeTableBody> bodies = component.getBodies();
        for (NativeTableBody body : bodies) {
            AssertUtils.assertEquals("Body is not a child of table", component,
                    body.getParent().orElseThrow());
        }
    }

    @Test
    public void removeBody() {
        var component = (NativeTable) getComponent();
        for (int i = 0; i < 10; i++) {
            component.addBody();
        }
        var bodies = component.getBodies();
        for (int i = 0; i < 10; i++) {
            component.removeBody();
            assertTrue(bodies.get(i).getParent().isEmpty());
        }
    }

    @Test
    public void removeBodyByIndex() {
        var component = (NativeTable) getComponent();
        var body0 = component.addBody();
        var body1 = component.addBody();
        var body2 = component.addBody();
        component.removeBody(1);
        assertTrue(body0.getParent().isPresent());
        assertTrue(body1.getParent().isEmpty());
        assertTrue(body2.getParent().isPresent());
    }

    @Test
    public void removeBodyByReference() {
        var component = (NativeTable) getComponent();
        var body0 = component.addBody();
        var body1 = component.addBody();
        var body2 = component.addBody();
        component.removeBody(body1);
        assertTrue(body0.getParent().isPresent());
        assertTrue(body1.getParent().isEmpty());
        assertTrue(body2.getParent().isPresent());
    }

}
