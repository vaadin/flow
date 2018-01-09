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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.ElementStateProvider;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ClientDelegateHandlers;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.ParentGeneratorHolder;
import com.vaadin.flow.internal.nodefeature.TemplateMap;
import com.vaadin.flow.internal.nodefeature.TemplateOverridesMap;
import com.vaadin.flow.template.angular.ElementTemplateBuilder;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TextTemplateBuilder;
import com.vaadin.flow.template.angular.parser.TemplateParser;
import com.vaadin.flow.template.angular.parser.TemplateResolver;

import elemental.json.Json;
import elemental.json.JsonObject;

public class TemplateElementStateProviderTest {

    public static class NullTemplateResolver implements TemplateResolver {
        @Override
        public InputStream resolve(String relativeFilename) throws IOException {
            throw new IOException("Null resolver is used");
        }
    }

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
    public void testElementStringProperties() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("a1", new StaticBindingValueProvider("v1"))
                .setProperty("a2", new StaticBindingValueProvider("v2"));

        Element element = createElement(builder);

        Assert.assertEquals("v1", element.getProperty("a1"));
        Assert.assertEquals("v2", element.getProperty("a2"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a1", "a2")),
                element.getPropertyNames().collect(Collectors.toSet()));
    }

    @Test
    public void testElementBooleanProperties() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("a", new ModelValueBindingProvider("key"));

        Element element = createElement(builder);

        StateNode stateNode = element.getNode();
        ModelMap.get(stateNode).setValue("key", Boolean.TRUE);

        Assert.assertEquals(Boolean.TRUE, element.getPropertyRaw("a"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a")),
                element.getPropertyNames().collect(Collectors.toSet()));
    }

    @Test
    public void testElementDoubleProperties() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("a", new ModelValueBindingProvider("key"));

        Element element = createElement(builder);

        StateNode stateNode = element.getNode();
        ModelMap.get(stateNode).setValue("key", 1.1d);

        Assert.assertEquals(1.1d, element.getPropertyRaw("a"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a")),
                element.getPropertyNames().collect(Collectors.toSet()));
    }

    @Test
    public void testElementJsonProperties() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("a", new ModelValueBindingProvider("key"));

        Element element = createElement(builder);

        StateNode stateNode = element.getNode();
        JsonObject json = Json.createObject();
        json.put("foo", "bar");
        ModelMap.get(stateNode).setValue("key", json);

        Assert.assertEquals(json, element.getPropertyRaw("a"));

        Assert.assertEquals(new HashSet<>(Arrays.asList("a")),
                element.getPropertyNames().collect(Collectors.toSet()));
    }

    @Test
    public void testElementAttributes() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setAttribute("a1", new StaticBindingValueProvider("v1"))
                .setAttribute("a2", new StaticBindingValueProvider("v2"));

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
                new StaticBindingValueProvider("Hello"));

        Element element = createElement(builder);

        Assert.assertTrue(element.isTextNode());
        Assert.assertEquals("Hello", element.getTextRecursively());
    }

    @Test
    public void testTextNodeInParent() {
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .addChild(new TextTemplateBuilder(
                        new StaticBindingValueProvider("Hello")));

        Element element = createElement(builder);

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals("Hello", element.getTextRecursively());

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

    @Test
    public void emptyChildSlot() {
        Element parent = createElement("<div>@child@</div>");

        Assert.assertEquals(0, parent.getChildCount());
    }

    @Test
    public void populatedChildSlot() {
        Element div = createElement("<div><span>@child@</span></div>");

        Element span = div.getChild(0);

        Element child = ElementFactory.createSpan("child");

        div.getNode().getFeature(TemplateMap.class).setChild(child.getNode());

        Assert.assertEquals(1, span.getChildCount());
        Assert.assertEquals(child, span.getChild(0));

        Assert.assertEquals(span, child.getParent());
    }

    @Test
    public void emptyChildSlotOrder() {
        Element parent = createElement(
                "<div><before></before>@child@<after></after></div>");

        Assert.assertEquals(2, parent.getChildCount());

        Assert.assertEquals(Arrays.asList("before", "after"),
                parent.getChildren().map(Element::getTag)
                        .collect(Collectors.toList()));
    }

    @Test
    public void populatedChildSlotOrder() {
        Element parent = createElement(
                "<div><before></before>@child@<after></after></div>");
        Element child = new Element("child");

        parent.getNode().getFeature(TemplateMap.class)
                .setChild(child.getNode());

        Assert.assertEquals(3, parent.getChildCount());

        Assert.assertEquals(Arrays.asList("before", "child", "after"),
                parent.getChildren().map(Element::getTag)
                        .collect(Collectors.toList()));
    }

    public void clearChildSlot_resetChild() {
        Element parent = createElement("<div>@child@</div>");
        Element child = ElementFactory.createSpan("child");

        TemplateMap templateMap = parent.getNode()
                .getFeature(TemplateMap.class);
        templateMap.setChild(child.getNode());

        Assert.assertEquals(1, parent.getChildCount());

        templateMap.setChild(null);

        Assert.assertEquals(0, parent.getChildCount());
        Assert.assertEquals(0, parent.getChildren().count());
        Assert.assertNull(child.getParent());
    }

    // Currently not implemented, but we might want to support this at some
    // point
    @Test(expected = IllegalStateException.class)
    public void clearChildSlot_removeElement() {
        Element parent = createElement("<div>@child@</div>");
        Element child = ElementFactory.createSpan("child");

        parent.getNode().getFeature(TemplateMap.class)
                .setChild(child.getNode());

        child.removeFromParent();
    }

    @Test
    public void textInChildSlot() {
        Element parent = createElement("<div>@child@</div>");
        Element child = Element.createText("The text");

        parent.getNode().getFeature(TemplateMap.class)
                .setChild(child.getNode());

        Assert.assertEquals(1, parent.getChildCount());
        Assert.assertEquals(1, parent.getChildren().count());
        Assert.assertEquals(parent, child.getParent());

        Assert.assertEquals("The text", parent.getTextRecursively());
    }

    @Test
    public void templateInChildSlot() {
        Element parent = createElement("<div>@child@</div>");
        Element child = createElement("<span>The text</span>");

        parent.getNode().getFeature(TemplateMap.class)
                .setChild(child.getNode());

        Assert.assertEquals(1, parent.getChildCount());
        Assert.assertEquals(1, parent.getChildren().count());
        Assert.assertEquals(parent, child.getParent());

        Assert.assertEquals("The text", parent.getTextRecursively());
    }

    @Test(expected = IllegalStateException.class)
    public void setChildWithoutSlot() {
        Element parent = createElement("<div>No child slot here</div>");
        Element child = ElementFactory.createDiv("child");

        parent.getNode().getFeature(TemplateMap.class)
                .setChild(child.getNode());
    }

    @Test
    public void testHardcodedStyleAttribute() {
        Element element = createElement("<div style='display:none'></div>");

        Assert.assertEquals(1, element.getAttributeNames().count());

        // Test the same after attributes have been migrated to an override node
        element.setProperty("foo", "bar");

        Assert.assertEquals(1, element.getAttributeNames().count());
    }

    @Test
    public void templateBoundClassAttribute() {
        Element element = createElement("<div class='foo bar'></div>");

        Assert.assertEquals("foo bar", element.getAttribute("class"));
        Assert.assertArrayEquals(new Object[] { "class" },
                element.getAttributeNames().toArray());

        assertClassList(element.getClassList(), "foo", "bar");

        // Test the same after attributes have been migrated to an override node
        element.setProperty("foo", "bar");

        Assert.assertEquals("foo bar", element.getAttribute("class"));
        Assert.assertArrayEquals(new Object[] { "class" },
                element.getAttributeNames().toArray());

        assertClassList(element.getClassList(), "foo", "bar");

    }

    @Test
    public void dynamicClassNames() {
        Element element = createElement(
                "<div class='foo' [class.bar]=hasBar [class.baz]=hasBaz></div>");
        ClassList classList = element.getClassList();

        // Explicitly set "hasBar" and "hasBaz" properties to null. So model has
        // properties "hasBar" and "hasBaz".
        // See #970
        element.getNode().getFeature(ModelMap.class).setValue("hasBar", null);
        element.getNode().getFeature(ModelMap.class).setValue("hasBaz", null);
        Assert.assertEquals("foo", element.getAttribute("class"));

        assertClassList(classList, "foo");
        assertNotClassList(classList, "bar", "baz");

        ModelMap modelMap = element.getNode().getFeature(ModelMap.class);

        modelMap.setValue("hasBar", "");
        modelMap.setValue("hasBaz", "yes");
        assertClassList(classList, "foo", "baz");
        assertNotClassList(classList, "bar");

        modelMap.setValue("hasBar", 5);
        modelMap.setValue("hasBaz", 0);
        assertClassList(classList, "foo", "bar");
        assertNotClassList(classList, "baz");

        modelMap.setValue("hasBar", false);
        modelMap.setValue("hasBaz", true);
        assertClassList(classList, "foo", "baz");
        assertNotClassList(classList, "bar");
    }

    @Test
    public void setProperty_regularProperty_elementDelegatesPropertyToOverrideNode() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");

        StateNode overrideNode = element.getNode()
                .getFeature(TemplateOverridesMap.class).get(node, false);
        Assert.assertTrue(BasicElementStateProvider.get()
                .hasProperty(overrideNode, "prop"));
        Assert.assertEquals("foo", BasicElementStateProvider.get()
                .getProperty(overrideNode, "prop"));
        List<String> props = BasicElementStateProvider.get()
                .getPropertyNames(overrideNode).collect(Collectors.toList());
        Assert.assertEquals(1, props.size());
        Assert.assertEquals("prop", props.get(0));
    }

    @Test
    public void setProperty_regularProperty_hasPropertyAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");

        Assert.assertTrue(element.hasProperty("prop"));
        Assert.assertEquals("foo", element.getProperty("prop"));
        List<String> props = element.getPropertyNames()
                .collect(Collectors.toList());
        Assert.assertEquals(1, props.size());
        Assert.assertEquals("prop", props.get(0));
    }

    @Test
    public void setRegularProperty_templateHasBoundProperty_hasPropertyAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div [foo]='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");

        Assert.assertTrue(element.hasProperty("prop"));
        Assert.assertEquals("foo", element.getProperty("prop"));
        Set<String> props = element.getPropertyNames()
                .collect(Collectors.toSet());
        Assert.assertEquals(2, props.size());
        Assert.assertTrue(props.contains("foo"));
        Assert.assertTrue(props.contains("prop"));
    }

    @Test
    public void setRegularProperty_templateHasAttribute_hasPropertyAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div foo='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("foo", "newValue");

        Assert.assertTrue(element.hasProperty("foo"));
        Assert.assertEquals("newValue", element.getProperty("foo"));
        Set<String> props = element.getPropertyNames()
                .collect(Collectors.toSet());
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.contains("foo"));
    }

    @Test
    public void removeRegularProperty_templateHasBoundProperty_hasPropertyAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div [foo]='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");

        element.removeProperty("prop");

        Assert.assertFalse(element.hasProperty("prop"));
        Set<String> props = element.getPropertyNames()
                .collect(Collectors.toSet());
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.contains("foo"));
    }

    @Test
    public void removeProperty_regularProperty_hasNoProperty() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");
        element.removeProperty("prop");

        Assert.assertFalse(element.hasProperty("prop"));
        List<String> props = element.getPropertyNames()
                .collect(Collectors.toList());
        Assert.assertEquals(0, props.size());
    }

    @Test
    public void removeProperty_templateHasAttribute_hasNoProperty() {
        TemplateNode node = TemplateParser.parse("<div foo='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        Assert.assertFalse(element.hasProperty("foo"));
        element.setProperty("foo", "bar");
        element.removeProperty("foo");

        Assert.assertFalse(element.hasProperty("foo"));
        List<String> props = element.getPropertyNames()
                .collect(Collectors.toList());
        Assert.assertEquals(0, props.size());
    }

    @Test
    public void removeProperty_regularProperty_elementDelegatesPropertyToOverrideNode() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setProperty("prop", "foo");
        element.removeProperty("prop");

        StateNode overrideNode = element.getNode()
                .getFeature(TemplateOverridesMap.class).get(node, false);
        Assert.assertFalse(BasicElementStateProvider.get()
                .hasProperty(overrideNode, "prop"));
        List<String> props = BasicElementStateProvider.get()
                .getPropertyNames(overrideNode).collect(Collectors.toList());
        Assert.assertEquals(0, props.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setProperty_boundProperty_throwException() {
        Element element = createElement("<div [prop]='value'></div>");
        element.setProperty("prop", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeProperty_boundProperty_throwException() {
        Element element = createElement("<div [prop]='value'></div>");
        element.removeProperty("prop");
    }

    @Test
    public void removeAttribute_templateHasAttribute_hasNoAttribute() {
        TemplateNode node = TemplateParser.parse("<div foo='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.removeAttribute("foo");

        Assert.assertFalse(element.hasAttribute("foo"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(0, attrs.size());
    }

    @Test
    public void removeAttribute_templateHasBoundAttribute_hasNoAttributeAndHasBoundAttribute() {
        TemplateNode node = TemplateParser.parse(
                "<div [attr.foo]='bar' attr='foo'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.removeAttribute("attr");

        Assert.assertFalse(element.hasAttribute("attr"));
        Assert.assertTrue(element.hasAttribute("foo"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(1, attrs.size());
    }

    @Test
    public void removeAttribute_templateHasOneMoreAttribute_hasNoAttribute() {
        TemplateNode node = TemplateParser.parse(
                "<div foo='bar' attr='value'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.removeAttribute("foo");

        Assert.assertFalse(element.hasAttribute("foo"));
        Assert.assertTrue(element.hasAttribute("attr"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(1, attrs.size());
        Assert.assertEquals("attr", attrs.get(0));
        Assert.assertEquals("value", element.getAttribute("attr"));
    }

    @Test
    public void removeAttribute_regularAttribute_hasNoAttribute() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");
        element.removeAttribute("attr");

        Assert.assertFalse(element.hasAttribute("attr"));
        List<String> props = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(0, props.size());
    }

    @Test
    public void removeAttribute_regularAttributeAndTemplateHasChildren_hasNoAttributeAndNoException() {
        TemplateNode node = TemplateParser.parse("<div><span></span></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");
        element.removeAttribute("attr");

        Assert.assertFalse(element.hasAttribute("attr"));
        List<String> props = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(0, props.size());
    }

    @Test
    public void setAttribute_regularAttribute_elementDelegatesAttributeToOverrideNode() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");

        StateNode overrideNode = element.getNode()
                .getFeature(TemplateOverridesMap.class).get(node, false);
        Assert.assertTrue(BasicElementStateProvider.get()
                .hasAttribute(overrideNode, "attr"));
        Assert.assertEquals("foo", BasicElementStateProvider.get()
                .getAttribute(overrideNode, "attr"));
        List<String> attrs = BasicElementStateProvider.get()
                .getAttributeNames(overrideNode).collect(Collectors.toList());
        Assert.assertEquals(1, attrs.size());
        Assert.assertEquals("attr", attrs.get(0));
    }

    @Test
    public void setAttribute_regularAttribute_hasAttributeAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");

        Assert.assertTrue(element.hasAttribute("attr"));
        Assert.assertEquals("foo", element.getAttribute("attr"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(1, attrs.size());
        Assert.assertEquals("attr", attrs.get(0));
    }

    @Test
    public void setAttribute_regularAttributeAndTemplateHasChildren_hasAttributeAndHasProperValueAndNoException() {
        TemplateNode node = TemplateParser.parse("<div><span></span></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");

        Assert.assertTrue(element.hasAttribute("attr"));
        Assert.assertEquals("foo", element.getAttribute("attr"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(1, attrs.size());
        Assert.assertEquals("attr", attrs.get(0));
    }

    @Test
    public void getBoundAttribute_setRegularAttribute_hasAttributeAndHasProperValue() {
        TemplateNode node = TemplateParser.parse(
                "<div [attr.attr]='foo'></div>", new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("foo", "bar");
        element.getNode().getFeature(ModelMap.class).setValue("foo",
                "someValue");

        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertTrue(element.hasAttribute("attr"));
        Assert.assertEquals("bar", element.getAttribute("foo"));
        Assert.assertEquals("someValue", element.getAttribute("attr"));
        List<String> attrs = element.getAttributeNames()
                .collect(Collectors.toList());
        Assert.assertEquals(2, attrs.size());
        Assert.assertTrue(attrs.contains("attr"));
        Assert.assertTrue(attrs.contains("foo"));
    }

    @Test
    public void setRegularAttribute_templateHasAttribute_hasAttributeAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div foo='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("foo", "newValue");

        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertEquals("newValue", element.getAttribute("foo"));
        Set<String> attrs = element.getAttributeNames()
                .collect(Collectors.toSet());
        Assert.assertEquals(1, attrs.size());
        Assert.assertTrue(attrs.contains("foo"));
    }

    @Test
    public void setRegularAttribute_templateHasDifferentAttribute_hasAttributeAndHasProperValue() {
        TemplateNode node = TemplateParser.parse("<div foo='bar'></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");

        Assert.assertTrue(element.hasAttribute("attr"));
        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertEquals("foo", element.getAttribute("attr"));
        Assert.assertEquals("bar", element.getAttribute("foo"));
        Set<String> attrs = element.getAttributeNames()
                .collect(Collectors.toSet());
        Assert.assertEquals(2, attrs.size());
        Assert.assertTrue(attrs.contains("foo"));
        Assert.assertTrue(attrs.contains("attr"));
    }

    @Test
    public void removeAttribute_regularAttribute_elementDelegatesAttributeToOverrideNode() {
        TemplateNode node = TemplateParser.parse("<div></div>",
                new NullTemplateResolver());
        Element element = createElement(node);
        element.setAttribute("attr", "foo");
        element.removeAttribute("attr");

        StateNode overrideNode = element.getNode()
                .getFeature(TemplateOverridesMap.class).get(node, false);
        Assert.assertFalse(BasicElementStateProvider.get()
                .hasAttribute(overrideNode, "attr"));
        List<String> attrs = BasicElementStateProvider.get()
                .getAttributeNames(overrideNode).collect(Collectors.toList());
        Assert.assertEquals(0, attrs.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAttribute_boundAttribute_throwException() {
        Element element = createElement("<div [attr.attr]='value'></div>");
        element.setAttribute("attr", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeAttribute_boundAttribute_throwException() {
        Element element = createElement("<div [attr.attr]='value'></div>");
        element.removeAttribute("attr");
    }

    private void assertClassList(ClassList classList, String... expectedNames) {
        Set<String> expectedSet = new HashSet<>(Arrays.asList(expectedNames));

        Assert.assertEquals(expectedNames.length, classList.size());
        Assert.assertEquals(expectedNames.length, classList.stream().count());
        Assert.assertEquals(expectedNames.length,
                iteratorToStream(classList.iterator()).count());

        for (String className : expectedNames) {
            Assert.assertTrue(classList.contains(className));
        }

        Assert.assertEquals(expectedSet, classList);
        Assert.assertEquals(classList, expectedSet);

        // Does classList.iterator() contain the right values?
        Assert.assertEquals(expectedSet, new HashSet<>(classList));

        // Does classList.stream() contain the right values?
        Assert.assertEquals(expectedSet,
                classList.stream().collect(Collectors.toSet()));
    }

    private Stream<String> iteratorToStream(Iterator<String> iterator) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    private void assertNotClassList(ClassList classList,
            String... forbiddenClassNames) {
        for (String className : forbiddenClassNames) {
            Assert.assertFalse(classList.contains(className));
        }

    }

    @Test(expected = UnsupportedOperationException.class)
    public void classListAddThrows() {
        // Not allowed until we explicitly support override node data for
        // ClassList
        createElement("<input>").getClassList().add("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void classListRemoveThrows() {
        createElement("<input class=foo>").getClassList().remove("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void classListIteratorRemoveThrows() {
        Iterator<String> iterator = createElement("<input class=foo>")
                .getClassList().iterator();
        iterator.next();
        iterator.remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requiredNodeFeatures() {
        Class<? extends NodeFeature>[] requiredFeatures = new Class[] {
                TemplateOverridesMap.class };

        TemplateElementStateProvider provider = (TemplateElementStateProvider) createElement(
                "<div></div").getStateProvider();

        // Test that a node with all required features is accepted
        Assert.assertTrue(provider.supports(new StateNode(requiredFeatures)));

        // Test that removing any feature makes it non-accepted
        for (int i = 0; i < requiredFeatures.length; i++) {
            List<Class<? extends NodeFeature>> list = new ArrayList<>(
                    Arrays.asList(requiredFeatures));
            list.remove(i);
            Assert.assertFalse(provider
                    .supports(new StateNode(list.toArray(new Class[0]))));
        }
    }

    @Test
    public void rootNodeFeatures() {
        assertHasFeatures(TemplateElementStateProvider.createRootNode(),
                ModelMap.class, TemplateOverridesMap.class, TemplateMap.class,
                ComponentMapping.class, ParentGeneratorHolder.class,
                ClientDelegateHandlers.class);
    }

    @Test
    public void subModelNodeFeatures() {
        assertHasFeatures(
                TemplateElementStateProvider.createSubModelNode(ModelMap.class),
                ModelMap.class, TemplateOverridesMap.class);
    }

    @SafeVarargs
    private static void assertHasFeatures(StateNode node,
            Class<? extends NodeFeature>... features) {
        Set<Class<? extends NodeFeature>> featureSet = new HashSet<>(
                Arrays.asList(features));

        for (Class<? extends NodeFeature> feature : NodeFeatureRegistry
                .getFeatures()) {
            boolean has = node.hasFeature(feature);
            if (featureSet.contains(feature)) {
                Assert.assertTrue("node should have the feature " + feature,
                        has);
            } else {
                Assert.assertFalse("node shouldn't have the feature " + feature,
                        has);
            }
        }
    }

    private static Element createElement(String template) {
        return createElement(
                TemplateParser.parse(template, new NullTemplateResolver()));
    }

    private static Element createElement(TemplateNodeBuilder builder) {
        List<TemplateNode> nodes = builder.build(null);
        Assert.assertEquals(1, nodes.size());
        return createElement(nodes.get(0));
    }

    public static Element createElement(TemplateNode templateNode) {
        StateNode stateNode = TemplateElementStateProvider.createRootNode();
        stateNode.getFeature(TemplateMap.class).setRootTemplate(templateNode);

        return Element.get(stateNode);
    }

    public static Optional<StateNode> getOverrideNode(Element element) {
        StateNode node = element.getNode();
        if (!node.hasFeature(TemplateOverridesMap.class)) {
            return Optional.empty();
        } else {
            ElementStateProvider stateProvider = element.getStateProvider();
            assert stateProvider instanceof TemplateElementStateProvider;
            return Optional.of(node.getFeature(TemplateOverridesMap.class)
                    .get(((TemplateElementStateProvider) stateProvider)
                            .getTemplateNode(), false));
        }
    }

    @Test
    public void testElementSubProperty() {
        String modelPath = "bean.name";
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("prop", new ModelValueBindingProvider(modelPath));

        Element element = createElement(builder);
        StateNode stateNode = element.getNode();
        ModelMap.get(stateNode).resolveModelMap("bean").setValue("name",
                "John");

        Assert.assertEquals("John", element.getProperty("prop"));
    }

    @Test
    public void templateMixedCaseAttributeProperty() {
        Element element = createElement(
                "<div [boundProperty]='modelParam' unboundAttribute='value' [attr.boundAttribute]='modelParam'></div>");

        ModelMap.get(element.getNode()).setValue("modelParam", "modelValue");

        // Attribute names are case insensitive
        Assert.assertEquals("value", element.getAttribute("unboundattribute"));
        Assert.assertEquals("value", element.getAttribute("UNBOUNDattribute"));
        Assert.assertEquals("modelValue",
                element.getAttribute("boundattribute"));
        Assert.assertEquals("modelValue",
                element.getAttribute("BOUNDattribute"));
        Assert.assertArrayEquals(
                new Object[] { "unboundattribute", "boundattribute" },
                element.getAttributeNames().toArray());

        // Property names are case sensitive
        Assert.assertEquals("modelValue", element.getProperty("boundproperty"));
        Assert.assertNull(element.getProperty("boundProperty"));
        Assert.assertArrayEquals(new Object[] { "boundproperty" },
                element.getPropertyNames().toArray());
    }

    @Test
    public void templateStaticStyleAttribute() {
        Element element = createElement("<div style='background:blue'></div>");

        Assert.assertEquals("background:blue", element.getAttribute("style"));
        Assert.assertEquals("blue", element.getStyle().get("background"));
        Assert.assertArrayEquals(new Object[] { "style" },
                element.getAttributeNames().toArray());
    }

    @Test
    public void templateStaticStyleGetUsingDashSeparated() {
        Element element = createElement("<div style='font-size:14px'></div>");

        Assert.assertEquals("14px", element.getStyle().get("font-size"));
        Assert.assertEquals("14px", element.getStyle().get("fontSize"));
        Assert.assertNull(element.getStyle().get("fontsize"));
    }

    @Test
    public void templateStaticStyleHasUsingDashSeparated() {
        Element element = createElement("<div style='font-size:14px'></div>");

        Assert.assertTrue(element.getStyle().has("font-size"));
        Assert.assertTrue(element.getStyle().has("fontSize"));
        Assert.assertFalse(element.getStyle().has("fontsize"));
    }

    @Test
    public void createSubModelNode_createdNodeHasRequiedFeature() {
        StateNode node = TemplateElementStateProvider
                .createSubModelNode(ModelList.class);

        Assert.assertTrue(node.isReportedFeature(ModelList.class));
    }

}
