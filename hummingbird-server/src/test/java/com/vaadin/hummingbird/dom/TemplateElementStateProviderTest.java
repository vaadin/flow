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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.template.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.StaticBinding;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TemplateParser;
import com.vaadin.hummingbird.template.TextTemplateBuilder;

public class TemplateElementStateProviderTest {
    @Test
    public void testEmptyElement() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div");

        Element element = createElement(builder);

        Assert.assertEquals("div", element.getTag());
        Assert.assertFalse(element.isTextNode());

        Assert.assertNull(element.getParent());
        Assert.assertEquals(0, element.getChildCount());

        Assert.assertEquals(0, element.getPropertyNames().count());
        Assert.assertEquals(0, element.getAttributeNames().count());
    }

    @Test
    public void testElementProperties() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("a1", new StaticBinding("v1"))
                .setProperty("a2", new StaticBinding("v2"));

        Element element = createElement(builder);

        Assert.assertEquals("v1", element.getProperty("a1"));
        Assert.assertEquals("v2", element.getProperty("a2"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a1", "a2")),
                element.getPropertyNames().collect(Collectors.toSet()));
    }

    @Test
    public void testElementAttributes() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setAttribute("a1", new StaticBinding("v1"))
                .setAttribute("a2", new StaticBinding("v2"));

        Element element = createElement(builder);

        Assert.assertEquals("v1", element.getAttribute("a1"));
        Assert.assertEquals("v2", element.getAttribute("a2"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a1", "a2")),
                element.getAttributeNames().collect(Collectors.toSet()));
    }

    @Test
    public void testTemplateInBasicElement() {
        Element templateElement = createElement(
                new ElementTemplateBuilder("template"));
        Element basicElement = new Element("basic");

        basicElement.appendChild(templateElement);

        Element child = basicElement.getChild(0);
        Assert.assertEquals("template", child.getTag());
        Assert.assertEquals(templateElement, child);

        Element parent = templateElement.getParent();
        Assert.assertEquals("basic", parent.getTag());
        Assert.assertEquals(basicElement, parent);
    }

    @Test
    public void testNestedTemplateElements() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("parent")
                .addChild(new ElementTemplateBuilder("child0"))
                .addChild(new ElementTemplateBuilder("child1"));

        Element element = createElement(builder);

        Assert.assertEquals(2, element.getChildCount());

        Element child0 = element.getChild(0);
        Assert.assertEquals("child0", child0.getTag());
        Assert.assertEquals(element, child0.getParent());

        Element child1 = element.getChild(1);
        Assert.assertEquals("child1", child1.getTag());
        Assert.assertEquals(element, child1.getParent());
    }

    @Test
    public void testTextNode() {
        TextTemplateBuilder builder = new TextTemplateBuilder(
                new StaticBinding("Hello"));

        Element element = createElement(builder);

        Assert.assertTrue(element.isTextNode());
        Assert.assertEquals("Hello", element.getTextContent());
    }

    @Test
    public void testTextNodeInParent() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .addChild(new TextTemplateBuilder(new StaticBinding("Hello")));

        Element element = createElement(builder);

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals("Hello", element.getTextContent());

        Element child = element.getChild(0);
        Assert.assertTrue(child.isTextNode());
        Assert.assertEquals(element, child.getParent());
    }

    @Test
    public void testAppendOverrideChild() {
        Element child = ElementFactory.createAnchor();

        Element parent = createElement("<div></div>");

        parent.appendChild(child);

        List<Element> children = parent.getChildren()
                .collect(Collectors.toList());

        Assert.assertEquals(1, children.size());

        Assert.assertEquals(child, children.get(0));

        Assert.assertEquals(parent, child.getParent());
    }

    @Test
    public void testRemoveOverrideChildByIndex() {
        Element child = ElementFactory.createAnchor();

        Element parent = createElement("<div></div>");

        parent.appendChild(child);

        parent.removeChild(0);

        Assert.assertEquals(0, parent.getChildCount());
        Assert.assertFalse(parent.getChildren().findFirst().isPresent());
    }

    @Test
    public void testRemoveOverrideChildByInstance() {
        Element child = new Element("a");

        Element parent = createElement("<div></div>");

        parent.appendChild(child);

        parent.removeChild(child);

        Assert.assertEquals(0, parent.getChildCount());
        Assert.assertFalse(parent.getChildren().findFirst().isPresent());
    }

    @Test
    public void testRemoveAllOverrideChildren() {
        Element child = ElementFactory.createAnchor();

        Element parent = createElement("<div></div>");

        parent.appendChild(child);

        parent.removeAllChildren();

        Assert.assertEquals(0, parent.getChildCount());
        Assert.assertFalse(parent.getChildren().findFirst().isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testAppendWithTemplateChildren() {
        Element parent = createElement("<div><span></span></div>");

        parent.appendChild(new Element("div"));
    }

    @Test(expected = IllegalStateException.class)
    public void testAppendWithTemplateText() {
        Element parent = createElement("<div>Text</div>");

        parent.appendChild(new Element("div"));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveByIndexWithTemplateChildren() {
        Element parent = createElement("<div><span></span></div>");

        parent.removeChild(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveByInstanceWithTemplateChildren() {
        Element parent = createElement("<div><span></span></div>");

        parent.removeChild(parent.getChild(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveAllWithTemplateChildren() {
        Element parent = createElement("<div><span></span></div>");

        parent.removeAllChildren();
    }

    private static Element createElement(String template) {
        return createElement(TemplateParser.parse(template));
    }

    private static Element createElement(TemplateNodeBuilder builder) {
        return createElement(builder.build(null));
    }

    private static Element createElement(TemplateNode templateNode) {
        StateNode stateNode = new StateNode(TemplateMap.class,
                TemplateOverridesMap.class);
        stateNode.getFeature(TemplateMap.class).setRootTemplate(templateNode);

        return Element.get(stateNode);
    }
}
