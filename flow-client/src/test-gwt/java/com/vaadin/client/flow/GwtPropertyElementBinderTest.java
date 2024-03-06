/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.binding.Debouncer;
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
            set(ApplicationConfiguration.class, new ApplicationConfiguration());
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

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();
        constantPool = new ConstantPool();
        tree = new CollectingStateTree(constantPool, new ExistingElementMap());

        node = createNode();
        properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        synchronizedPropertyList = new NodeList(0, node);

        element = Browser.getDocument().createElement("div");
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

    public void testFlushPendingChangesOnDomEvent() {
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

        AtomicInteger commandExecution = new AtomicInteger();
        Debouncer debouncer = Debouncer.getOrCreate(element, "on-value:false",
                300);
        debouncer.trigger(JsCollections.<String> set()
                .add(JsonConstants.EVENT_PHASE_TRAILING), phase -> {
                    if (phase == null) {
                        commandExecution.incrementAndGet();
                    } else if (JsonConstants.EVENT_PHASE_TRAILING
                            .equals(phase)) {
                        finishTest();
                    }
                });

        String constantPoolKey = "expressionsKey";
        JsonObject expressions = Json.createObject();
        expressions.put(
                JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "offsetWidth",
                false);
        GwtBasicElementBinderTest.addToConstantPool(constantPool,
                constantPoolKey, expressions);
        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("event1")
                .setValue(constantPoolKey);
        Reactive.flush();

        dispatchEvent("event1");

        assertEquals("Changes should have not been flushed", 0,
                commandExecution.get());

        // Wait for debouncer to be unregistered
        delayTestFinish(1000);
    }

    public void testDoNotFlushPendingChangesOnPropertySynchronization() {
        Browser.getDocument().getBody().appendChild(element);
        Binder.bind(node, element);

        AtomicInteger commandExecution = new AtomicInteger();
        Debouncer debouncer = Debouncer.getOrCreate(element, "on-value:false",
                300);
        debouncer.trigger(JsCollections.<String> set()
                .add(JsonConstants.EVENT_PHASE_TRAILING), phase -> {
                    if (phase == null) {
                        commandExecution.incrementAndGet();
                    } else if (JsonConstants.EVENT_PHASE_TRAILING
                            .equals(phase)) {
                        finishTest();
                    }
                });

        String constantPoolKey = "expressionsKey";
        JsonObject expressions = Json.createObject();
        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("event1")
                .setValue(constantPoolKey);
        GwtBasicElementBinderTest.addToConstantPool(constantPool,
                constantPoolKey, expressions);

        Reactive.flush();

        dispatchEvent("event1");

        assertEquals("Changes should have been flushed", 1,
                commandExecution.get());

        // Wait for debouncer to be unregistered
        delayTestFinish(1000);
    }

    protected StateNode createNode() {
        return new StateNode(0, tree);
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
