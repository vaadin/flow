/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.nodefeature.TextNodeMap;

public class TextElementStateProviderTest {
    private Element element = Element.createText("foo");
    private TextNodeMap feature = element.getNode()
            .getFeature(TextNodeMap.class);

    @Test
    public void testBlankNode() {
        Assert.assertTrue(element.isTextNode());

        Assert.assertEquals("foo", element.getTextContent());

        Assert.assertEquals("foo", feature.getText());
    }

    @Test
    public void testElementReadsFeature() {
        feature.setText("bar");

        Assert.assertEquals("bar", element.getTextContent());
    }

    @Test
    public void testSetTextContent() {
        element.setTextContent("bar");

        Assert.assertEquals("bar", element.getTextContent());
        Assert.assertEquals("bar", feature.getText());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testElementProperties() {
        element.getPropertyNames();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetUndefinedProperty() {
        element.getProperty("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHasUndefinedProperty() {
        element.hasProperty("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setSetUndefinedPropertyThrows() {
        element.setProperty("foo", "bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAttributes() {
        element.getAttributeNames();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetUndefinedAttribute() {
        element.getAttribute("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHasUndefinedAttribute() {
        element.hasAttribute("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetAttributeThrows() {
        element.setAttribute("foo", "bar");
    }

    @Test
    public void testZeroChildren() {
        Assert.assertEquals(0, element.getChildCount());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addChildThrows() {
        element.appendChild(ElementFactory.createDiv());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTagThrows() {
        element.getTag();
    }

    @Test
    public void testTextChild() {
        Element parent = ElementFactory.createDiv();
        parent.appendChild(element);

        Assert.assertEquals(parent, element.getParent());
        Assert.assertEquals(element, parent.getChild(0));

        element.removeFromParent();
        Assert.assertNull(element.getParent());
        Assert.assertEquals(0, parent.getChildCount());
    }
}
