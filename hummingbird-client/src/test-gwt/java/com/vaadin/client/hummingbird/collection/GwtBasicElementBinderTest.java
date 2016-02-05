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
package com.vaadin.client.hummingbird.collection;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.BasicElementBinder;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.namespace.ListNamespace;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.NodeList;

public class GwtBasicElementBinderTest extends ClientEngineTestBase {
    private StateNode node = new StateTree().getRootNode();

    private MapNamespace properties = node
            .getMapNamespace(Namespaces.ELEMENT_PROPERTIES);
    private MapNamespace attributes = node
            .getMapNamespace(Namespaces.ELEMENT_ATTRIBUTES);
    private MapNamespace elementData = node
            .getMapNamespace(Namespaces.ELEMENT_DATA);
    private ListNamespace children = node
            .getListNamespace(Namespaces.ELEMENT_CHILDREN);

    private MapProperty titleProperty = properties.getProperty("title");
    private MapProperty idAttribute = attributes.getProperty("id");

    private int nextId = node.getId() + 1;

    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        element = Browser.getDocument().createElement("div");
    }

    public void testBindExistingProperty() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getTitle());
    }

    public void testBindNewProperty() {
        BasicElementBinder.bind(node, element);

        properties.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testBindingBeforeFlush() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        assertEquals("", element.getTitle());
    }

    public void testUnbindBeforeFlush() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        binder.remove();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");

        Reactive.flush();

        assertEquals("", element.getTitle());
        assertEquals("", element.getId());
    }

    public void testUnbindAfterFlush() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        Reactive.flush();

        binder.remove();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");

        Reactive.flush();

        assertEquals("foo", element.getTitle());
        assertEquals("foo", element.getId());
    }

    public void testRemoveArbitraryProperty() {
        MapProperty foo = properties.getProperty("foo");
        foo.setValue("bar");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertTrue(WidgetUtil.hasOwnJsProperty(element, "foo"));

        foo.removeValue();

        Reactive.flush();

        assertFalse(WidgetUtil.hasOwnJsProperty(element, "foo"));
    }

    public void testRemoveBuiltInProperty() {
        titleProperty.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        titleProperty.removeValue();

        Reactive.flush();

        // Properties inherited from e.g. Element can't be removed
        // Assigning null to title produces "null"
        assertEquals("null", element.getTitle());
    }

    public void testBindWrongTagThrows() {
        elementData.getProperty(Namespaces.TAG).setValue("span");

        try {
            BasicElementBinder.bind(node, element);
            fail("Should have thrown");
        } catch (AssertionError expected) {
        }
    }

    public void testBindRightTagOk() {
        elementData.getProperty(Namespaces.TAG).setValue("div");

        BasicElementBinder.bind(node, element);
    }

    public void testBindExistingAttribute() {
        idAttribute.setValue("foo");

        BasicElementBinder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getId());
    }

    public void testBindNewAttribute() {
        BasicElementBinder.bind(node, element);

        attributes.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testSetAttributeWithoutFlush() {
        idAttribute.setValue("foo");

        BasicElementBinder.bind(node, element);

        assertEquals("", element.getId());
    }

    public void restRemoveAttribute() {
        BasicElementBinder.bind(node, element);

        idAttribute.setValue("foo");

        Reactive.flush();

        idAttribute.removeValue();

        Reactive.flush();

        assertEquals(null, element.getId());
    }

    private StateNode createChildNode(String id) {
        StateNode childNode = new StateNode(nextId++, node.getTree());

        childNode.getMapNamespace(Namespaces.ELEMENT_DATA)
                .getProperty(Namespaces.TAG).setValue("span");
        if (id != null) {
            childNode.getMapNamespace(Namespaces.ELEMENT_ATTRIBUTES)
                    .getProperty("id").setValue(id);
        }

        return childNode;
    }

    public void testAddChild() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = createChildNode("child");

        children.splice(0, 0, childNode);

        Reactive.flush();

        assertEquals(element.getChildElementCount(), 1);

        Element childElement = element.getFirstElementChild();

        assertEquals("SPAN", childElement.getTagName());
        assertEquals("child", childElement.getId());
    }

    public void testAddChildPosition() {
        BasicElementBinder.bind(node, element);

        StateNode firstChildNode = createChildNode("first");
        children.splice(0, 0, firstChildNode);
        Reactive.flush();

        Element firstChildElement = element.getFirstElementChild();

        // Add an "unofficial" child to mess with index computations
        Element extraChild = Browser.getDocument().createElement("img");
        element.insertBefore(extraChild, firstChildElement);

        StateNode secondChildNode = createChildNode("second");
        children.splice(1, 0, secondChildNode);
        Reactive.flush();

        Element secondChild = element.querySelector("#second");
        assertNotNull(secondChild);

        NodeList childNodes = element.getChildNodes();

        assertEquals(3, childNodes.length());

        assertSame(extraChild, childNodes.item(0));
        assertSame(firstChildElement, childNodes.item(1));
        assertSame(secondChild, childNodes.item(2));
    }

    public void testRemoveChild() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = createChildNode(null);

        children.splice(0, 0, childNode);

        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
        Element childElement = element.getFirstElementChild();

        children.splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildElementCount());
        assertNull(childElement.getParentElement());
    }

    public void testRemoveChildPosition() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = createChildNode("child");
        children.splice(0, 0, childNode);
        Reactive.flush();

        Element firstChildElement = element.getFirstElementChild();

        // Add an "unofficial" child to mess with index computations
        Element extraChild = Browser.getDocument().createElement("img");
        element.insertBefore(extraChild, firstChildElement);

        children.splice(0, 1);
        Reactive.flush();

        NodeList childNodes = element.getChildNodes();

        assertEquals(1, childNodes.length());

        assertSame(extraChild, childNodes.item(0));
        assertNull(firstChildElement.getParentElement());
    }

    public void testAddRemoveMultiple() {
        BasicElementBinder.bind(node, element);

        children.splice(0, 0, createChildNode("1"), createChildNode("2"),
                createChildNode("3"), createChildNode("4"));

        Reactive.flush();

        assertEquals(4, element.getChildElementCount());

        Element child1 = (Element) element.getChildren().item(0);
        Element child2 = (Element) element.getChildren().item(1);
        Element child3 = (Element) element.getChildren().item(2);
        Element child4 = (Element) element.getChildren().item(3);

        assertEquals("1", child1.getId());
        assertEquals("2", child2.getId());
        assertEquals("3", child3.getId());
        assertEquals("4", child4.getId());

        children.splice(1, 2);

        assertEquals(2, element.getChildElementCount());

        assertSame(child1, element.getChildNodes().item(0));
        assertSame(child4, element.getChildNodes().item(1));
    }

    public void testAddBeforeSetTag() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = new StateNode(nextId++, node.getTree());

        children.splice(0, 0, childNode);

        childNode.getMapNamespace(Namespaces.ELEMENT_DATA)
                .getProperty(Namespaces.TAG).setValue("span");

        // Should not throw
        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
    }
}
