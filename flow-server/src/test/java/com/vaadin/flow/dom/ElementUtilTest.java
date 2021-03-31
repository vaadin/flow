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
package com.vaadin.flow.dom;

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;

public class ElementUtilTest {
    @Test
    public void isNullValidAttribute() {
        Assert.assertFalse(ElementUtil.isValidAttributeName(null));
    }

    @Test
    public void isEmptyValidAttribute() {
        Assert.assertFalse(ElementUtil.isValidAttributeName(""));
    }

    @Test(expected = AssertionError.class)
    public void isUpperCaseValidAttribute() {
        // isValidAttributeName is designed to only be called with lowercase
        // attribute names
        ElementUtil.isValidAttributeName("FOO");
    }

    @Test
    public void componentNotInitiallyAttached() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.getComponent().isPresent());
    }

    @Test
    public void attachToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        Assert.assertEquals(c, e.getComponent().get());
    }

    @Test
    public void attachComponentToTextElement() {
        Element e = Element.createText("Text text");
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        Assert.assertEquals(c, e.getComponent().get());
    }

    @Test(expected = IllegalStateException.class)
    public void attachTwiceToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        ElementUtil.setComponent(e, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachToNull() {
        Element e = ElementFactory.createDiv();
        ElementUtil.setComponent(e, null);
    }

    @Test(expected = IllegalStateException.class)
    public void attachTwoComponents() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        Component c2 = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        ElementUtil.setComponent(e, c2);
    }

    @Test
    public void toAndFromJsoup() {
        final String EXPECTED_TEXT_1 = "Some text";
        final String EXPECTED_TEXT_2 = "Other text";

        Element originalElement = ElementFactory.createDiv();
        originalElement.appendChild(
                ElementFactory.createParagraph(EXPECTED_TEXT_1).appendChild(
                        ElementFactory.createDiv(EXPECTED_TEXT_2)));

        Document jDocument = Document.createShell("http://example.com");

        Node jNode = ElementUtil.toJsoup(jDocument, originalElement);

        Optional<Element> optionalElement = ElementUtil.fromJsoup(jNode);

        Assert.assertTrue("Element should have been created from jNode",
                optionalElement.isPresent());

        Element recreatedElement = optionalElement.get();

        // root
        Assert.assertEquals("Root element should be div", "div",
                recreatedElement.getTag());
        // child
        Assert.assertEquals("Child element should be a paragraph", "p",
                recreatedElement.getChild(0).getTag());
        Assert.assertEquals("Child element should have text", EXPECTED_TEXT_1,
                recreatedElement.getChild(0).getText());
        // grand-child (#1, since #0 is the text node)
        Assert.assertEquals("Grand-child element should be a div", "div",
                recreatedElement.getChild(0).getChild(1).getTag());
        Assert.assertEquals("Grand-child element should have text",
                EXPECTED_TEXT_2,
                recreatedElement.getChild(0).getChild(1).getText());
    }
}
