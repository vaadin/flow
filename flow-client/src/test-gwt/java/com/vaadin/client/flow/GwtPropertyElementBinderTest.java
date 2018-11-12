/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.client.flow;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.shared.JsonConstants;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class GwtPropertyElementBinderTest
        extends ClientEngineTestBase {

    private static class TestRegistry extends Registry {
        private InitialPropertiesHandler handler = new InitialPropertiesHandler(
                this);

        private ConstantPool constantPool;
        private ExistingElementMap existingElementMap;

        TestRegistry(ConstantPool constantPool,
                ExistingElementMap existingElementMap) {
            this.constantPool = constantPool;
            this.existingElementMap = existingElementMap;
        }

        @Override
        public ConstantPool getConstantPool() {
            return constantPool;
        }

        @Override
        public ExistingElementMap getExistingElementMap() {
            return existingElementMap;
        }

        @Override
        public InitialPropertiesHandler getInitialPropertiesHandler() {
            return handler;
        }

        private void setTree(StateTree tree) {
            set(StateTree.class, tree);
        }
    }

    protected static class CollectingStateTree extends StateTree {
        JsArray<StateNode> collectedNodes = JsCollections.array();
        JsArray<JsonObject> collectedEventData = JsCollections.array();
        JsMap<StateNode, JsMap<String, Object>> synchronizedProperties = JsCollections
                .map();
        List<Object> existingElementRpcArgs = new ArrayList<>();

        public CollectingStateTree(ConstantPool constantPool,
                ExistingElementMap existingElementMap) {
            super(new TestRegistry(constantPool, existingElementMap));
            ((TestRegistry) getRegistry()).setTree(this);
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

        @Override
        public void sendExistingElementWithIdAttachToServer(StateNode parent,
                int requestedId, int assignedId, String id) {
            existingElementRpcArgs.add(parent);
            existingElementRpcArgs.add(requestedId);
            existingElementRpcArgs.add(assignedId);
            existingElementRpcArgs.add(id);
        }

        public void clearSynchronizedProperties() {
            synchronizedProperties.clear();
        }
    }

    protected CollectingStateTree tree;

    protected ConstantPool constantPool;

    protected StateNode node;

    protected Element element;

    protected NodeMap properties;
    private NodeList synchronizedPropertyList;
    private NodeList synchronizedPropertyEventsList;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();
        constantPool = new ConstantPool();
        tree = new CollectingStateTree(constantPool, new ExistingElementMap());

        node = createNode();
        properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        synchronizedPropertyList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);
        synchronizedPropertyEventsList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        element = Browser.getDocument().createElement("div");
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

    public void testDomListenerSynchronization() {
        // Must append for events to work in HTMLUnit
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

        setSyncProperties("offsetHeight");

        String constantPoolKey = "expressionsKey";

        JsonObject expressions = Json.createObject();
        boolean isFilter = false;
        expressions.put(
                JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "offsetWidth",
                isFilter);

        GwtBasicElementBinderTest.addToConstantPool(constantPool,
                constantPoolKey, expressions);
        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("event1")
                .setValue(constantPoolKey);
        Reactive.flush();

        element.getStyle().setWidth("2px");
        element.getStyle().setHeight("2px");
        dispatchEvent("event1");
        /*
         * Only offsetWidth should be synchronized. offsetHeight is also marked
         * as a globally synchronized property, but it should not be sent since
         * there's no global synchronization configured for the event, only
         * synchronization of one specific property.
         */
        assertSynchronized("offsetWidth");
    }

    protected StateNode createNode() {
        return new StateNode(0, tree);
    }

    private void setSyncEvents(String... eventTypes) {
        synchronizedPropertyEventsList.splice(0,
                synchronizedPropertyEventsList.length());
        for (int i = 0; i < eventTypes.length; i++) {
            synchronizedPropertyEventsList.add(i, eventTypes[i]);
        }
    }

    private void setSyncProperties(String... properties) {
        synchronizedPropertyList.splice(0, synchronizedPropertyList.length());
        for (int i = 0; i < properties.length; i++) {
            synchronizedPropertyList.add(i, properties[i]);
        }
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
