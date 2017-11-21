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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.ui.Component;

public class ElementUtilTest {
    @Test
    public void isNullValidAttribute() {
        assertFalse(ElementUtil.isValidAttributeName(null));
    }

    @Test
    public void isEmptyValidAttribute() {
        assertFalse(ElementUtil.isValidAttributeName(""));
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
        assertFalse(ElementUtil.getComponent(e).isPresent());
    }

    @Test
    public void attachToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertEquals(c, ElementUtil.getComponent(e).get());
    }

    @Test
    public void attachComponentToTextElement() {
        Element e = Element.createText("Text text");
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertEquals(c, ElementUtil.getComponent(e).get());
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
    public void includesScriptTags() {
        InlineTemplate template = new InlineTemplate(
                "<div><script>window.alert('shazbot');</script></div>");
        Node jsoupNode = ElementUtil.toJsoup(new Document(""),
                template.getElement());

        assertEquals(1, jsoupNode.childNodeSize());

        Node child = jsoupNode.childNode(0);
        assertEquals("<script>window.alert('shazbot');</script>",
                child.outerHtml());
    }

}
