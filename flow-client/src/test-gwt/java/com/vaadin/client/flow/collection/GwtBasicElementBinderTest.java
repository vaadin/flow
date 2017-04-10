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
package com.vaadin.client.flow.collection;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.template.TemplateRegistry;
import com.vaadin.client.flow.template.TestElementTemplateNode;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class GwtBasicElementBinderTest extends ClientEngineTestBase {

    private static final String PROPERTY_PREFIX = "_";

    private static class CollectingStateTree extends StateTree {
        JsArray<StateNode> collectedNodes = JsCollections.array();
        JsArray<JsonObject> collectedEventData = JsCollections.array();
        JsMap<StateNode, JsMap<String, Object>> synchronizedProperties = JsCollections
                .map();

        public CollectingStateTree(ConstantPool constantPool) {
            super(new Registry() {
                @Override
                public ConstantPool getConstantPool() {
                    return constantPool;
                }
            });
        }

        @Override
        public void sendEventToServer(StateNode node, String eventType,
                JsonObject eventData) {
            collectedNodes.push(node);
            collectedEventData.push(eventData);
        }

        @Override
        public void sendNodePropertySyncToServer(MapProperty property) {
            StateNode node = property.getMap().getNode();
            String propertyName = property.getName();
            Object value = property.getValue();

            if (!synchronizedProperties.has(node)) {
                synchronizedProperties.set(node, JsCollections.map());
            }
            JsMap<String, Object> nodeMap = synchronizedProperties.get(node);
            assertFalse(nodeMap.has(propertyName));
            nodeMap.set(propertyName, value);
        }

        public void clearSynchronizedProperties() {
            synchronizedProperties.clear();
        }
    }

    private ConstantPool constantPool;

    private CollectingStateTree tree;

    private StateNode node;

    private NodeMap properties;
    private NodeMap attributes;
    private NodeMap elementData;
    private NodeList children;
    private NodeList synchronizedPropertyList;
    private NodeList synchronizedPropertyEventsList;

    private MapProperty titleProperty;
    private MapProperty idAttribute;

    private int nextId;

    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();
        constantPool = new ConstantPool();
        tree = new CollectingStateTree(constantPool);

        node = new StateNode(0, tree);
        properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        attributes = node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        elementData = node.getMap(NodeFeatures.ELEMENT_DATA);
        children = node.getList(NodeFeatures.ELEMENT_CHILDREN);
        synchronizedPropertyList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);
        synchronizedPropertyEventsList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        // initialize style feature
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);

        titleProperty = properties.getProperty("title");
        idAttribute = attributes.getProperty("id");

        nextId = node.getId() + 1;

        element = Browser.getDocument().createElement("div");
    }

    public void testBindExistingProperty() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getTitle());
    }

    public void testBindNewProperty() {
        Binder.bind(node, element);

        properties.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testBindingBeforeFlush() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        assertEquals("", element.getTitle());
    }

    public void testUnbindBeforeFlush() {
        Binder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        node.unregister();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");
        attributes.getProperty("lang").setValue("newValue");

        Reactive.flush();

        assertEquals("", element.getTitle());
        assertEquals("", element.getId());
        assertEquals("", element.getLang());
    }

    public void testUnbindAfterFlush() {
        Binder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        Reactive.flush();

        node.unregister();

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

        Binder.bind(node, element);

        Reactive.flush();

        assertTrue(WidgetUtil.hasOwnJsProperty(element, "foo"));

        foo.removeValue();

        Reactive.flush();

        assertFalse(WidgetUtil.hasOwnJsProperty(element, "foo"));
    }

    public void testRemoveBuiltInProperty() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        titleProperty.removeValue();

        Reactive.flush();

        // Properties inherited from e.g. Element can't be removed
        // Assigning null to title produces "null"
        assertEquals("null", element.getTitle());
    }

    public void testBindWrongTagThrows() {
        elementData.getProperty(NodeFeatures.TAG).setValue("span");

        try {
            Binder.bind(node, element);
            fail("Should have thrown");
        } catch (AssertionError expected) {
        }
    }

    public void testBindRightTagOk() {
        elementData.getProperty(NodeFeatures.TAG).setValue("div");

        Binder.bind(node, element);
    }

    public void testBindExistingAttribute() {
        idAttribute.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getId());
    }

    public void testBindNewAttribute() {
        Binder.bind(node, element);

        attributes.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testSetAttributeWithoutFlush() {
        idAttribute.setValue("foo");

        Binder.bind(node, element);

        assertEquals("", element.getId());
    }

    public void testRemoveAttribute() {
        Binder.bind(node, element);

        idAttribute.setValue("foo");

        Reactive.flush();

        idAttribute.removeValue();

        Reactive.flush();

        assertEquals("", element.getId());
    }

    private StateNode createChildNode(String id) {
        StateNode childNode = new StateNode(nextId++, node.getTree());

        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("span");
        if (id != null) {
            childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty("id")
                    .setValue(id);
        }

        return childNode;
    }

    public void testAddChild() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");

        children.add(0, childNode);

        Reactive.flush();

        assertEquals(element.getChildElementCount(), 1);

        Element childElement = element.getFirstElementChild();

        assertEquals("SPAN", childElement.getTagName());
        assertEquals("child", childElement.getId());
    }

    public void testRemoveChild() {
        Binder.bind(node, element);

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
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");
        children.add(0, childNode);
        Reactive.flush();

        Element firstChildElement = element.getFirstElementChild();

        // Add an "unofficial" child to mess with index computations
        Element extraChild = Browser.getDocument().createElement("img");
        element.insertBefore(extraChild, firstChildElement);

        children.splice(0, 1);
        Reactive.flush();

        elemental.dom.NodeList childNodes = element.getChildNodes();

        assertEquals(1, childNodes.length());

        assertSame(extraChild, childNodes.item(0));
        assertNull(firstChildElement.getParentElement());
    }

    public void testAddRemoveMultiple() {
        Binder.bind(node, element);

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
        Binder.bind(node, element);

        StateNode childNode = new StateNode(nextId++, node.getTree());

        children.add(0, childNode);

        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("span");

        // Should not throw
        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
    }

    public void testReAddNode() {
        Binder.bind(node, element);

        StateNode childToReadd = createChildNode("2");
        children.splice(0, 0, JsCollections.array(createChildNode("1"),
                childToReadd, createChildNode("3")));

        Reactive.flush();

        Node node = childToReadd.getDomNode();

        assertEquals(3, element.getChildElementCount());

        children.splice(1, 1);
        children.splice(1, 0, JsCollections.array(childToReadd));

        Reactive.flush();

        StateNode statNode = (StateNode) children.get(1);
        assertSame(childToReadd, statNode);
        assertSame(node, statNode.getDomNode());
    }

    public void testEventFired() {
        Binder.bind(node, element);

        // User agent is "Mozilla/5.0..."
        String booleanExpression = "window.navigator.userAgent[0] === 'M'";
        String numberExpression = "event.button";
        String stringExpression = "element.tagName";

        String constantPoolKey = "expressionsKey";

        JsonArray expressionConstantValue = Json.createArray();
        expressionConstantValue.set(0, booleanExpression);
        expressionConstantValue.set(1, numberExpression);
        expressionConstantValue.set(2, stringExpression);

        addToConstantPool(constantPoolKey, expressionConstantValue);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
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

    private void addToConstantPool(String key, JsonValue value) {
        JsonObject update = Json.createObject();
        update.put(key, value);
        constantPool.importFromJson(update);
    }

    public void testRemovedEventNotFired() {
        Binder.bind(node, element);

        MapProperty clickEvent = node.getMap(NodeFeatures.ELEMENT_LISTENERS)
                .getProperty("click");
        clickEvent.setValue(Double.valueOf(1));

        Reactive.flush();

        clickEvent.removeValue();

        Reactive.flush();

        element.click();

        assertEquals(0, tree.collectedNodes.length());
    }

    public void testAddTextNode() {
        Binder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        MapProperty textProperty = textNode.getMap(NodeFeatures.TEXT_NODE)
                .getProperty(NodeFeatures.TEXT);

        textProperty.setValue("foo");

        node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals("foo", element.getTextContent());

        textProperty.setValue("bar");
        assertEquals("foo", element.getTextContent());

        Reactive.flush();
        assertEquals("bar", element.getTextContent());
    }

    public void testRemoveTextNode() {
        Binder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        textNode.getMap(NodeFeatures.TEXT_NODE).getProperty(NodeFeatures.TEXT)
                .setValue("foo");

        node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals(1, element.getChildNodes().getLength());

        node.getList(NodeFeatures.ELEMENT_CHILDREN).splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildNodes().getLength());
    }

    public void testAddClassesBeforeBind() {
        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        Binder.bind(node, element);

        assertEquals("foo", element.getClassName());
    }

    public void testAddClassesAfterBind() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        assertEquals("foo", element.getClassName());
    }

    public void testRemoveClasses() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).splice(0, 0,
                JsCollections.array("one", "two", "three"));

        assertEquals("one two three", element.getClassName());

        node.getList(NodeFeatures.CLASS_LIST).splice(1, 1);

        assertEquals("one three", element.getClassName());
    }

    public void testAddClassesAfterUnbind() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        node.unregister();

        node.getList(NodeFeatures.CLASS_LIST).add(0, "bar");

        assertEquals("foo", element.getClassName());
    }

    public void testAddStylesBeforeBind() {
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        Binder.bind(node, element);

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testAddStylesAfterBind() {
        Binder.bind(node, element);
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testRemoveStyles() {
        Binder.bind(node, element);

        NodeMap styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
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
        Binder.bind(node, element);

        NodeMap styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);

        styleMap.getProperty("color").setValue("red");
        Reactive.flush();

        node.unregister();

        styleMap.getProperty("color").setValue("blue");
        styleMap.getProperty("font-size").setValue("12px");

        Reactive.flush();
        assertEquals("color: red;", element.getAttribute("style"));
    }

    private void setSyncEvents(NodeList syncEventList, String... eventTypes) {
        syncEventList.splice(0, syncEventList.length());
        for (int i = 0; i < eventTypes.length; i++) {
            syncEventList.add(i, eventTypes[i]);
        }
    }

    private void setSyncEvents(String... eventTypes) {
        setSyncEvents(synchronizedPropertyEventsList, eventTypes);
    }

    private void setSyncProperties(NodeList syncProps, String... properties) {
        syncProps.splice(0, syncProps.length());
        for (int i = 0; i < properties.length; i++) {
            syncProps.add(i, properties[i]);
        }
    }

    private void setSyncProperties(String... properties) {
        setSyncProperties(synchronizedPropertyList, properties);
    }

    public void testSynchronizePropertySendsToServer() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

        setSyncEvents("event1");
        setSyncProperties("offsetWidth", "tagName");
        Reactive.flush();

        assertSynchronized();
        dispatchEvent("event1");
        assertSynchronized("offsetWidth", "tagName");
    }

    public void testPolymerSynchronizePropertySendsToServer() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);

        node = new StateNode(1, tree);

        // initialize node with appropriate features
        node.getMap(NodeFeatures.ELEMENT_DATA);
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        NodeList syncEvents = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        NodeList syncProprties = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);

        Binder.bind(node, element);

        setSyncEvents(syncEvents, "event1");
        setSyncProperties(syncProprties, "offsetWidth", "tagName");
        Reactive.flush();

        assertSynchronized();
        dispatchEvent("event1");
        assertSynchronized("offsetWidth", "tagName");
    }

    public void testSynchronizePropertyOnlyOnChange() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

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

    public void testPolymerSynchronizePropertyOnlyOnChange() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);

        node = new StateNode(1, tree);

        // initialize node with appropriate features
        node.getMap(NodeFeatures.ELEMENT_DATA);
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        NodeList syncEvents = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        NodeList syncProprties = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);

        Binder.bind(node, element);

        setSyncEvents(syncEvents, "event");
        setSyncProperties(syncProprties, "offsetWidth", "offsetHeight");
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
        Binder.bind(node, element);

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

        synchronizedPropertyEventsList.splice(0,
                synchronizedPropertyEventsList.length());
        dispatchEvent("event2");
        assertSynchronized();

    }

    public void testPolymerSynchronizePropertyAddRemoveEvent() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);

        node = new StateNode(1, tree);

        // initialize node with appropriate features
        node.getMap(NodeFeatures.ELEMENT_DATA);
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        NodeList syncEvents = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        NodeList syncProprties = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);

        Binder.bind(node, element);

        setSyncEvents(syncEvents, "event1", "event2");
        setSyncProperties(syncProprties, "offsetWidth");
        Reactive.flush();

        setSyncEvents(syncEvents, "event2");
        Reactive.flush();

        dispatchEvent("event1");
        assertSynchronized();
        tree.clearSynchronizedProperties();
        dispatchEvent("event2");
        assertSynchronized("offsetWidth");
        tree.clearSynchronizedProperties();

        synchronizedPropertyEventsList.splice(0,
                synchronizedPropertyEventsList.length());
        dispatchEvent("event2");
        assertSynchronized();

    }

    public void testSynchronizePropertyAddRemoveProperties() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

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

    public void testPolymerSynchronizePropertyAddRemoveProperties() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);

        // make function "set" available for the element
        WidgetUtil.setJsProperty(element, "set", NativeFunction.create(""));

        node = new StateNode(1, tree);

        // initialize node with appropriate features
        node.getMap(NodeFeatures.ELEMENT_DATA);
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        NodeList syncEvents = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        NodeList syncProprties = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);

        Binder.bind(node, element);

        setSyncEvents(syncEvents, "event1");
        setSyncProperties(syncProprties, "offsetWidth");
        Reactive.flush();

        element.getStyle().setHeight("1px");
        element.getStyle().setWidth("1px");
        dispatchEvent("event1");
        assertSynchronized("offsetWidth");
        tree.clearSynchronizedProperties();

        setSyncProperties(syncProprties, "offsetWidth", "offsetHeight");
        Reactive.flush();

        element.getStyle().setHeight("2px");
        element.getStyle().setWidth("2px");
        dispatchEvent("event1");
        assertSynchronized("offsetWidth", "offsetHeight");
        tree.clearSynchronizedProperties();

        setSyncProperties(syncProprties);
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

    public void testAddTemplateChild() {
        final int templateId = 43;
        TestElementTemplateNode templateNode = TestElementTemplateNode
                .create("child");

        TemplateRegistry templates = new TemplateRegistry();

        templates.register(templateId, templateNode);

        Registry registry = new Registry() {
            {
                set(TemplateRegistry.class, templates);
            }
        };

        StateTree stateTree = new StateTree(registry);

        StateNode templateStateNode = new StateNode(345, stateTree);
        templateStateNode.getMap(NodeFeatures.TEMPLATE)
                .getProperty(NodeFeatures.ROOT_TEMPLATE_ID)
                .setValue(Double.valueOf(templateId));

        StateNode parentElementNode = new StateNode(94, stateTree);
        parentElementNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).setValue("div");
        parentElementNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0,
                templateStateNode);

        Element element = Browser.getDocument().createElement("div");
        Binder.bind(parentElementNode, element);

        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
        assertEquals("CHILD", element.getFirstElementChild().getTagName());
    }

    public void testPropertyAdded() {
        setupSetMethod(PROPERTY_PREFIX);

        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";

        setModelProperty(node, propertyName, propertyValue);

        assertEquals(propertyValue, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + propertyName));
    }

    public void testPropertyUpdated() {
        setupSetMethod(PROPERTY_PREFIX);

        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);
        String newValue = "tea";

        setModelProperty(node, propertyName, newValue);

        assertEquals(newValue, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + propertyName));
    }

    public void testUnregister() {
        setupSetMethod(PROPERTY_PREFIX);

        Binder.bind(node, element);
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);
        String notUpdatedValue = "bubblegum";

        node.unregister();
        setModelProperty(node, propertyName, notUpdatedValue);

        assertEquals(propertyValue, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + propertyName));
    }

    public void testSetSubProperty() {
        setupSetMethod(PROPERTY_PREFIX);

        String property = "model";

        StateTree stateTree = new StateTree(new Registry());
        StateNode modelNode = new StateNode(34, stateTree);
        setModelProperty(node, property, modelNode, false);

        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, false);

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals(value, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + property + "." + subProperty));
    }

    public void testUpdateSubProperty() {
        setupSetMethod(PROPERTY_PREFIX);

        Binder.bind(node, element);

        String property = "model";

        StateTree stateTree = new StateTree(new Registry());
        StateNode modelNode = new StateNode(34, stateTree);
        setModelProperty(node, property, modelNode, false);

        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, true);

        // the property is set to an empty object '{}' at this point, reset it
        // it null to make sure it is not updated
        WidgetUtil.setJsProperty(element, PROPERTY_PREFIX + property, null);

        String newValue = "bar";
        setModelProperty(modelNode, subProperty, newValue, true);

        assertEquals(newValue, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + property + "." + subProperty));
        // Now check that the value for the property has not been updated
        assertEquals(null,
                WidgetUtil.getJsProperty(element, PROPERTY_PREFIX + property));
    }

    public void testSubPropertyUnregister() {
        setupSetMethod(PROPERTY_PREFIX);

        Binder.bind(node, element);

        String property = "model";

        StateTree stateTree = new StateTree(new Registry());
        StateNode modelNode = new StateNode(34, stateTree);
        setModelProperty(node, property, modelNode, false);

        String subProperty = "subProp";
        String value = "foo";
        setModelProperty(modelNode, subProperty, value, true);
        node.unregister();
        modelNode.unregister();

        setModelProperty(modelNode, subProperty, "bar", true);

        assertEquals(value, WidgetUtil.getJsProperty(element,
                PROPERTY_PREFIX + property + "." + subProperty));
    }

    public void testNoPropertiesFeature_propertiesMapIsNotCreated() {
        StateNode node = new StateNode(1, tree);
        // recognize the node as a polymer template
        node.getMap(NodeFeatures.ELEMENT_DATA);
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        Binder.bind(node, element);

        assertFalse(node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES));
    }

    private void setupSetMethod(String prefix) {
        // make sure that model feature is in the node
        node.getMap(NodeFeatures.TEMPLATE_MODELMAP);

        NativeFunction function = NativeFunction
                .create("this['" + prefix + "'+arguments[0]]=arguments[1]");
        WidgetUtil.setJsProperty(element, "set", function);
    }

    private static void setModelProperty(StateNode stateNode, String name,
            Object value, boolean flush) {
        stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(name)
                .setValue(value);
        if (flush) {
            Reactive.flush();
        }
    }

    private static void setModelProperty(StateNode stateNode, String name,
            Object value) {
        setModelProperty(stateNode, name, value, true);
    }

}
