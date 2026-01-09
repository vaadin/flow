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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;

public class ClickEventTest {

    @Test
    public void serverSideConstructor() {
        Component component = new Component(new Element("div")) {};
        ClickEvent<Component> event = new ClickEvent<>(component);
        
        Assert.assertEquals(component, event.getSource());
        Assert.assertFalse(event.isFromClient());
        
        // All coordinates should be -1 for server-side events
        Assert.assertEquals(-1, event.getScreenX());
        Assert.assertEquals(-1, event.getScreenY());
        Assert.assertEquals(-1, event.getClientX());
        Assert.assertEquals(-1, event.getClientY());
        Assert.assertEquals(-1, event.getRelativeX());
        Assert.assertEquals(-1, event.getRelativeY());
        
        Assert.assertEquals(1, event.getClickCount());
        Assert.assertEquals(-1, event.getButton());
        Assert.assertFalse(event.isCtrlKey());
        Assert.assertFalse(event.isShiftKey());
        Assert.assertFalse(event.isAltKey());
        Assert.assertFalse(event.isMetaKey());
    }

    @Test
    public void clientSideConstructor() {
        Component component = new Component(new Element("div")) {};
        ClickEvent<Component> event = new ClickEvent<>(component, true,
            100, 200, // screen coordinates
            150, 250, // client coordinates  
            10, 20,   // relative coordinates
            2,        // click count
            0,        // button (left mouse button)
            true, false, true, false); // modifier keys

        Assert.assertEquals(component, event.getSource());
        Assert.assertTrue(event.isFromClient());
        
        Assert.assertEquals(100, event.getScreenX());
        Assert.assertEquals(200, event.getScreenY());
        Assert.assertEquals(150, event.getClientX());
        Assert.assertEquals(250, event.getClientY());
        Assert.assertEquals(10, event.getRelativeX());
        Assert.assertEquals(20, event.getRelativeY());
        
        Assert.assertEquals(2, event.getClickCount());
        Assert.assertEquals(0, event.getButton());
        Assert.assertTrue(event.isCtrlKey());
        Assert.assertFalse(event.isShiftKey());
        Assert.assertTrue(event.isAltKey());
        Assert.assertFalse(event.isMetaKey());
    }

    @Test
    public void oldClientSideConstructorBackwardCompatibility() {
        Component component = new Component(new Element("div")) {};
        // Test the old constructor without relative coordinates
        ClickEvent<Component> event = new ClickEvent<>(component, true,
            100, 200, // screen coordinates
            150, 250, // client coordinates  
            2,        // click count
            0,        // button (left mouse button)
            true, false, true, false); // modifier keys

        Assert.assertEquals(component, event.getSource());
        Assert.assertTrue(event.isFromClient());
        
        Assert.assertEquals(100, event.getScreenX());
        Assert.assertEquals(200, event.getScreenY());
        Assert.assertEquals(150, event.getClientX());
        Assert.assertEquals(250, event.getClientY());
        
        // Relative coordinates should be -1 when using old constructor
        Assert.assertEquals(-1, event.getRelativeX());
        Assert.assertEquals(-1, event.getRelativeY());
        
        Assert.assertEquals(2, event.getClickCount());
        Assert.assertEquals(0, event.getButton());
        Assert.assertTrue(event.isCtrlKey());
        Assert.assertFalse(event.isShiftKey());
        Assert.assertTrue(event.isAltKey());
        Assert.assertFalse(event.isMetaKey());
    }

    @Test
    public void relativeCoordinatesAreDistinctFromOtherCoordinates() {
        Component component = new Component(new Element("div")) {};
        ClickEvent<Component> event = new ClickEvent<>(component, true,
            100, 200, // screen coordinates
            150, 250, // client coordinates  
            10, 20,   // relative coordinates (different from screen/client)
            1, 0, false, false, false, false);

        // Verify that relative coordinates are different from screen and client coordinates
        Assert.assertNotEquals(event.getScreenX(), event.getRelativeX());
        Assert.assertNotEquals(event.getScreenY(), event.getRelativeY());
        Assert.assertNotEquals(event.getClientX(), event.getRelativeX());
        Assert.assertNotEquals(event.getClientY(), event.getRelativeY());
        
        // Verify the actual values
        Assert.assertEquals(10, event.getRelativeX());
        Assert.assertEquals(20, event.getRelativeY());
    }
}