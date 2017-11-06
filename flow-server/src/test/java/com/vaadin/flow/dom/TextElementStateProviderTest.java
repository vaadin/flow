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
package com.vaadin.flow.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.nodefeature.TextNodeMap;

public class TextElementStateProviderTest {
    private Element element = Element.createText("foo");
    private TextNodeMap feature = element.getNode()
            .getFeature(TextNodeMap.class);

    @Test
    public void testBlankNode() {
        Assert.assertTrue(element.isTextNode());

        Assert.assertEquals("foo", element.getTextRecursively());

        Assert.assertEquals("foo", feature.getText());
    }

    @Test
    public void testElementReadsFeature() {
        feature.setText("bar");

        Assert.assertEquals("bar", element.getTextRecursively());
    }

    @Test
    public void testSetTextContent() {
        element.setText("bar");

        Assert.assertEquals("bar", element.getTextRecursively());
        Assert.assertEquals("bar", feature.getText());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setSetUndefinedPropertyThrows() {
        element.setProperty("foo", "bar");
    }

    @Test
    public void textNodeHasAttribute() {
        Assert.assertFalse(Element.createText("foo").hasAttribute("bar"));
    }

    @Test
    public void textNodeGetAttribute() {
        Assert.assertNull(Element.createText("foo").getAttribute("bar"));
    }

    @Test
    public void textNodeGetAttributeNames() {
        Assert.assertEquals(0,
                Element.createText("foo").getAttributeNames().count());
    }

    @Test
    public void textNodeHasProperty() {
        Assert.assertFalse(Element.createText("foo").hasProperty("bar"));
    }

    @Test
    public void textNodeGetProperty() {
        Assert.assertNull(Element.createText("foo").getProperty("bar"));
    }

    @Test
    public void textNodeGetPropertyNames() {
        Assert.assertEquals(0,
                Element.createText("foo").getPropertyNames().count());
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
