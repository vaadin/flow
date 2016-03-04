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
import com.vaadin.hummingbird.namespace.SynchronizedPropertiesNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class GwtBasicElementBinderTest extends ClientEngineTestBase {
    private static class CollectingStateTree extends StateTree {
        JsArray<StateNode> collectedNodes = JsCollections.array();
        JsArray<JsonObject> collectedEventData = JsCollections.array();
        JsMap<StateNode, JsMap<String, Object>> synchronizedProperties = JsCollections
                .map();

        public CollectingStateTree() {
            super(null);
        }

        @Override
        public void sendEventToServer(StateNode node, String eventType,
                JsonObject eventData) {
            collectedNodes.push(node);
            collectedEventData.push(eventData);
        }

        @Override
        public void sendPropertySyncToServer(StateNode node, String property,
                Object value) {
            if (!synchronizedProperties.has(node)) {
                synchronizedProperties.set(node, JsCollections.map());
            }
            JsMap<String, Object> nodeMap = synchronizedProperties.get(node);
            assertFalse(nodeMap.has(property));
            nodeMap.set(property, value);
        }

        public void clearSynchronizedProperties() {
            synchronizedProperties.clear();
        }
    }

    private CollectingStateTree tree;

    private StateNode node;

    private MapNamespace properties;
    private MapNamespace attributes;
    private MapNamespace elementData;
    private ListNamespace children;
    private MapNamespace synchronizedPropertyNamespace;

    private MapProperty titleProperty;
    private MapProperty idAttribute;

    private int nextId;

    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();
        tree = new CollectingStateTree();

        node = tree.getRootNode();
        properties = node.getMapNamespace(Namespaces.ELEMENT_PROPERTIES);
        attributes = node.getMapNamespace(Namespaces.ELEMENT_ATTRIBUTES);
        elementData = node.getMapNamespace(Namespaces.ELEMENT_DATA);
        children = node.getListNamespace(Namespaces.ELEMENT_CHILDREN);
        synchronizedPropertyNamespace = node
                .getMapNamespace(Namespaces.SYNCHRONIZED_PROPERTIES);

        titleProperty = properties.getProperty("title");
        idAttribute = attributes.getProperty("id");

        nextId = node.getId() + 1;

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
        attributes.getProperty("lang").setValue("newValue");

        Reactive.flush();

        assertEquals("", element.getTitle());
        assertEquals("", element.getId());
        assertEquals("", element.getLang());
    }

    public void testUnbindAfterFlush() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        Reactive.flush();

        binder.remove();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");
        attributes.getProperty("lang").setValue("newValue");

        Reactive.flush();

        assertEquals("foo", element.getTitle());
        assertEquals("foo", element.getId());
        assertEquals("", element.getLang());
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

    public void testRemoveAttribute() {
        BasicElementBinder.bind(node, element);

        idAttribute.setValue("foo");

        Reactive.flush();

        idAttribute.removeValue();

        Reactive.flush();

        assertEquals("", element.getId());
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

        children.add(0, childNode);

        Reactive.flush();

        assertEquals(element.getChildElementCount(), 1);

        Element childElement = element.getFirstElementChild();

        assertEquals("SPAN", childElement.getTagName());
        assertEquals("child", childElement.getId());
    }

    public void testRemoveChild() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = createChildNode(null);

        children.add(0, childNode);

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
        children.add(0, childNode);
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

        children.splice(0, 0,
                JsCollections.array(createChildNode("1"), createChildNode("2"),
                        createChildNode("3"), createChildNode("4")));

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

        Reactive.flush();

        assertEquals(2, element.getChildElementCount());

        assertSame(child1, element.getChildNodes().item(0));
        assertSame(child4, element.getChildNodes().item(1));
    }

    public void testAddBeforeSetTag() {
        BasicElementBinder.bind(node, element);

        StateNode childNode = new StateNode(nextId++, node.getTree());

        children.add(0, childNode);

        childNode.getMapNamespace(Namespaces.ELEMENT_DATA)
                .getProperty(Namespaces.TAG).setValue("span");

        // Should not throw
        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
    }

    public void testEventFired() {
        BasicElementBinder.bind(node, element);

        // User agent is "Mozilla/5.0..."
        String booleanExpression = "window.navigator.userAgent[0] === 'M'";
        String numberExpression = "event.button";
        String stringExpression = "element.tagName";

        node.getMapNamespace(Namespaces.ELEMENT_LISTENERS).getProperty("click")
                .setValue(JsCollections.array(booleanExpression,
                        numberExpression, stringExpression));
        Reactive.flush();
        Browser.getDocument().getBody().appendChild(element);

        element.click();

        assertEquals(1, tree.collectedNodes.length());

        assertSame(node, tree.collectedNodes.get(0));

        JsonObject eventData = tree.collectedEventData.get(0);

        assertEquals(3, eventData.keys().length);

        assertEquals(JsonType.NUMBER,
                eventData.get(numberExpression).getType());
        assertEquals("DIV", eventData.getString(stringExpression));
        assertEquals(true, eventData.getBoolean(booleanExpression));
    }

    public void testRemovedEventNotFired() {
        BasicElementBinder.bind(node, element);

        MapProperty clickEvent = node
                .getMapNamespace(Namespaces.ELEMENT_LISTENERS)
                .getProperty("click");
        clickEvent.setValue(Double.valueOf(1));

        Reactive.flush();

        clickEvent.removeValue();

        Reactive.flush();

        element.click();

        assertEquals(0, tree.collectedNodes.length());
    }

    public void testAddTextNode() {
        BasicElementBinder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        MapProperty textProperty = textNode
                .getMapNamespace(Namespaces.TEXT_NODE)
                .getProperty(Namespaces.TEXT);

        textProperty.setValue("foo");

        node.getListNamespace(Namespaces.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals("foo", element.getTextContent());

        textProperty.setValue("bar");
        assertEquals("foo", element.getTextContent());

        Reactive.flush();
        assertEquals("bar", element.getTextContent());
    }

    public void testRemoveTextNode() {
        BasicElementBinder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        textNode.getMapNamespace(Namespaces.TEXT_NODE)
                .getProperty(Namespaces.TEXT).setValue("foo");

        node.getListNamespace(Namespaces.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals(1, element.getChildNodes().getLength());

        node.getListNamespace(Namespaces.ELEMENT_CHILDREN).splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildNodes().getLength());
    }

    public void testAddClassesBeforeBind() {
        node.getListNamespace(Namespaces.CLASS_LIST).add(0, "foo");

        BasicElementBinder.bind(node, element);

        assertEquals("foo", element.getClassName());
    }

    public void testAddClassesAfterBind() {
        BasicElementBinder.bind(node, element);

        node.getListNamespace(Namespaces.CLASS_LIST).add(0, "foo");

        assertEquals("foo", element.getClassName());
    }

    public void testRemoveClasses() {
        BasicElementBinder.bind(node, element);

        node.getListNamespace(Namespaces.CLASS_LIST).splice(0, 0,
                JsCollections.array("one", "two", "three"));

        assertEquals("one two three", element.getClassName());

        node.getListNamespace(Namespaces.CLASS_LIST).splice(1, 1);

        assertEquals("one three", element.getClassName());
    }

    public void testAddClassesAfterUnbind() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        node.getListNamespace(Namespaces.CLASS_LIST).add(0, "foo");

        binder.remove();

        node.getListNamespace(Namespaces.CLASS_LIST).add(0, "bar");

        assertEquals("foo", element.getClassName());
    }

    public void testAddStylesBeforeBind() {
        node.getMapNamespace(Namespaces.ELEMENT_STYLE_PROPERTIES)
                .getProperty("color").setValue("green");

        Reactive.flush();
        BasicElementBinder.bind(node, element);

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testAddStylesAfterBind() {
        BasicElementBinder.bind(node, element);
        node.getMapNamespace(Namespaces.ELEMENT_STYLE_PROPERTIES)
                .getProperty("color").setValue("green");

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testRemoveStyles() {
        BasicElementBinder.bind(node, element);

        MapNamespace styleMap = node
                .getMapNamespace(Namespaces.ELEMENT_STYLE_PROPERTIES);
        styleMap.getProperty("background").setValue("blue");
        styleMap.getProperty("color").setValue("white");

        Reactive.flush();
        assertEquals("background: blue; color: white;",
                element.getAttribute("style"));

        styleMap.getProperty("color").removeValue();

        Reactive.flush();
        assertEquals("background: blue;", element.getAttribute("style"));
    }

    public void testAddStylesAfterUnbind() {
        BasicElementBinder binder = BasicElementBinder.bind(node, element);

        MapNamespace styleMap = node
                .getMapNamespace(Namespaces.ELEMENT_STYLE_PROPERTIES);

        styleMap.getProperty("color").setValue("red");
        Reactive.flush();

        binder.remove();

        styleMap.getProperty("color").setValue("blue");
        styleMap.getProperty("font-size").setValue("12px");

        Reactive.flush();
        assertEquals("color: red;", element.getAttribute("style"));
    }

    private void setSyncEvents(String... eventTypes) {
        synchronizedPropertyNamespace
                .getProperty(SynchronizedPropertiesNamespace.KEY_EVENTS)
                .setValue(JsCollections.array(eventTypes));
    }

    private void setSyncProperties(String... properties) {
        synchronizedPropertyNamespace
                .getProperty(SynchronizedPropertiesNamespace.KEY_PROPERTIES)
                .setValue(JsCollections.array(properties));
    }

    public void testSynchronizePropertySendsToServer() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        BasicElementBinder.bind(node, element);

        setSyncEvents("event1");
        setSyncProperties("offsetWidth", "tagName");
        Reactive.flush();

        assertSynchronized();
        dispatchEvent("event1");
        assertSynchronized("offsetWidth", "tagName");
    }

    public void testSynchronizePropertyOnlyOnChange() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        BasicElementBinder.bind(node, element);

        setSyncEvents("event");
        setSyncProperties("offsetWidth", "offsetHeight");
        Reactive.flush();

        dispatchEvent("event");
        assertSynchronized("offsetWidth", "offsetHeight");
        tree.clearSynchronizedProperties();

        dispatchEvent("event");
        assertSynchronized();
        tree.clearSynchronizedProperties();

        element.getStyle().setWidth("123px");
        dispatchEvent("event");
        assertSynchronized("offsetWidth");
        tree.clearSynchronizedProperties();

        element.getStyle().setHeight("123px");
        dispatchEvent("event");
        assertSynchronized("offsetHeight");
    }

    public void testSynchronizePropertyAddRemoveEvent() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        BasicElementBinder.bind(node, element);

        setSyncEvents("event1", "event2");
        setSyncProperties("offsetWidth");
        Reactive.flush();

        setSyncEvents("event2");
        Reactive.flush();

        dispatchEvent("event1");
        assertSynchronized();
        tree.clearSynchronizedProperties();
        dispatchEvent("event2");
        assertSynchronized("offsetWidth");
        tree.clearSynchronizedProperties();

        synchronizedPropertyNamespace
                .getProperty(SynchronizedPropertiesNamespace.KEY_EVENTS)
                .removeValue();
        dispatchEvent("event2");
        assertSynchronized();

    }

    public void testSynchronizePropertyAddRemoveProperties() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        BasicElementBinder.bind(node, element);

        setSyncEvents("event1");
        setSyncProperties("offsetWidth");
        Reactive.flush();

        element.getStyle().setHeight("1px");
        element.getStyle().setWidth("1px");
        dispatchEvent("event1");
        assertSynchronized("offsetWidth");
        tree.clearSynchronizedProperties();

        setSyncProperties("offsetWidth", "offsetHeight");
        Reactive.flush();

        element.getStyle().setHeight("2px");
        element.getStyle().setWidth("2px");
        dispatchEvent("event1");
        assertSynchronized("offsetWidth", "offsetHeight");
        tree.clearSynchronizedProperties();

        setSyncProperties();
        Reactive.flush();
        element.getStyle().setHeight("3px");
        element.getStyle().setWidth("3px");
        dispatchEvent("event1");
        assertSynchronized();
        tree.clearSynchronizedProperties();
    }

    private void dispatchEvent(String eventType) {
        element.dispatchEvent(createEvent(eventType));
    }

    private static native Event createEvent(String type)
    /*-{
        return new Event(type);
     }-*/;

    private void assertSynchronized(String... properties) {
        if (properties.length == 0) {
            assertEquals(0, tree.synchronizedProperties.size());
        } else {
            assertEquals(1, tree.synchronizedProperties.size());
            tree.synchronizedProperties.forEach((v, k) -> {
                assertEquals(node, k);
                assertEquals(properties.length, v.size());
                for (String property : properties) {
                    assertTrue(v.has(property));
                }
            });
        }
    }

}
