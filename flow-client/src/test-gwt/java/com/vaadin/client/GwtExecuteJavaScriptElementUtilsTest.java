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
package com.vaadin.client;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.dom.Element;

public class GwtExecuteJavaScriptElementUtilsTest extends ClientEngineTestBase {

    private StateNode node;

    private Element element;

    private ExistingElementStateTree tree;

    private ExistingElementMap map;

    private int nextId = 10;

    private int requestedId = 5;

    private static class ExistingElementStateTree extends StateTree {

        private StateNode sentExistingElementParent;

        private int sentExistingElementRequestedId;

        private int sentExistingElementAssignedId;

        private String sentExistingElementTagName;

        private int sentExistingElementIndex;

        private String sentExistingElementId;

        private MapProperty syncedProperty;

        ExistingElementStateTree(Registry registry) {
            super(registry);
        }

        @Override
        public void sendExistingElementAttachToServer(StateNode parent,
                int requestedId, int assignedId, String tagName, int index) {
            sentExistingElementParent = parent;
            sentExistingElementRequestedId = requestedId;
            sentExistingElementAssignedId = assignedId;
            sentExistingElementTagName = tagName;
            sentExistingElementIndex = index;
        }

        @Override
        public void sendExistingElementWithIdAttachToServer(StateNode parent,
                int requestedId, int assignedId, String id) {
            sentExistingElementParent = parent;
            sentExistingElementRequestedId = requestedId;
            sentExistingElementAssignedId = assignedId;
            sentExistingElementId = id;
        }

        @Override
        public void sendNodePropertySyncToServer(MapProperty property) {
            syncedProperty = property;
        }
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        initPolymer();

        map = new ExistingElementMap();

        Registry registry = new Registry() {
            {
                set(ExistingElementMap.class, map);
            }
        };

        tree = new ExistingElementStateTree(registry);

        node = new StateNode(0, tree);
        element = Browser.getDocument().createElement("div");
        node.setDomNode(element);

    }

    public void testAttachExistingElement_noSibling() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        addChildElement("a");
        Element child3 = addChildElement("button");

        ExecuteJavaScriptElementUtils.attachExistingElement(node, null,
                "button", requestedId);

        assertRpcToServerArguments(requestedId, child3.getTagName(), 1);
    }

    public void testAttachExistingElement_siblingIdProvided() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        Element child2 = addChildElement("a");
        addChild(child2, 1);

        addChildElement("span");
        Element child4 = addChildElement("button");

        ExecuteJavaScriptElementUtils.attachExistingElement(node, child2,
                "button", requestedId);

        assertRpcToServerArguments(requestedId, child4.getTagName(), 2);
    }

    public void testAttachExistingElement_elementHasServersideCounterpart() {
        Element child1 = addChildElement("span");

        StateNode childNode = addChild(child1, 0);

        addChildElement("button");

        ExecuteJavaScriptElementUtils.attachExistingElement(node, null, "span",
                requestedId);

        assertRpcToServerArguments(childNode.getId(), child1.getTagName(), 0);
    }

    public void testAttachExistingElement_elementIsAlreadyAssociated() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        Element child2 = addChildElement("button");

        int associatedId = 13;
        map.add(associatedId, child2);

        ExecuteJavaScriptElementUtils.attachExistingElement(node, null,
                "button", requestedId);

        assertRpcToServerArguments(associatedId, child2.getTagName(), 1);
    }

    public void testAttachExistingElement_noRequestedElement() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        ExecuteJavaScriptElementUtils.attachExistingElement(node, null,
                "button", requestedId);

        assertRpcToServerArguments(-1, "button", -1);
    }

    public void testPopulateModelProperties_propertyIsNotDefined_addIntoPropertiesMap() {
        ExecuteJavaScriptElementUtils.populateModelProperties(node,
                JsCollections.array("foo"));

        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        assertTrue(map.hasPropertyValue("foo"));
    }

    public void testPopulateModelProperties_elementIsNotReadyAndPropertyIsNotDefined_addIntoPropertiesMapAfterElementBecomesReady() {
        node.setDomNode(null);

        mockWhenDefined(element);

        ExecuteJavaScriptElementUtils.populateModelProperties(node,
                JsCollections.array("foo"));

        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        assertFalse(map.hasPropertyValue("foo"));

        node.setDomNode(element);
        runWhenDefined(element);
        Reactive.flush();

        assertTrue(map.hasPropertyValue("foo"));
    }

    public void testPopulateModelProperties_propertyIsDefined_syncToServer() {
        defineProperty(element, "foo");

        WidgetUtil.setJsProperty(element, "foo", "bar");

        ExecuteJavaScriptElementUtils.populateModelProperties(node,
                JsCollections.array("foo"));

        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        assertTrue(map.hasPropertyValue("foo"));

        assertEquals("bar", tree.syncedProperty.getValue());
        assertEquals("foo", tree.syncedProperty.getName());
    }

    public void testPopulateModelProperties_elementIsNotReadyAndPropertyIsDefined_syncToServerWhenElementBecomesReady() {
        node.setDomNode(null);

        mockWhenDefined(element);

        defineProperty(element, "foo");

        WidgetUtil.setJsProperty(element, "foo", "bar");

        ExecuteJavaScriptElementUtils.populateModelProperties(node,
                JsCollections.array("foo"));

        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        assertFalse(map.hasPropertyValue("foo"));

        node.setDomNode(element);
        runWhenDefined(element);
        Reactive.flush();

        assertTrue(map.hasPropertyValue("foo"));

        assertEquals("bar", tree.syncedProperty.getValue());
        assertEquals("foo", tree.syncedProperty.getName());
    }

    private void setupShadowRoot() {
        setupParent(element);

        NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        map.getProperty(NodeProperties.SHADOW_ROOT)
                .setValue(new StateNode(34, tree));
    }

    private native void setupParent(Element element)
    /*-{
        element.$ = function() {
            return element;
        };
        element.root = element;
    }-*/;

    private native void addChildElement(Element parent, Element child)
    /*-{
        parent.$[child.getAttribute("id")] = child;
    }-*/;

    private void assertRpcToServerArguments(int associatedId, String tagName,
            int index) {
        assertEquals(node.getId(), tree.sentExistingElementParent.getId());
        assertEquals(requestedId, tree.sentExistingElementRequestedId);
        assertEquals(associatedId, tree.sentExistingElementAssignedId);
        assertEquals(tagName, tree.sentExistingElementTagName);
        assertEquals(index, tree.sentExistingElementIndex);
    }

    private void assertRpcToServerArguments(int associatedId, String tagName,
            String id) {
        assertEquals(node.getId(), tree.sentExistingElementParent.getId());
        assertEquals(requestedId, tree.sentExistingElementRequestedId);
        assertEquals(associatedId, tree.sentExistingElementAssignedId);
        assertTrue(tagName.equalsIgnoreCase(tree.sentExistingElementTagName));
        assertEquals(id, tree.sentExistingElementId);
    }

    private StateNode addChild(Element child, int index) {
        StateNode childNode = new StateNode(nextId, tree);
        nextId++;
        childNode.setDomNode(child);
        node.getList(NodeFeatures.ELEMENT_CHILDREN).add(index, childNode);
        return childNode;
    }

    private Element addChildElement(String tag) {
        Element child = Browser.getDocument().createElement(tag);
        element.appendChild(child);
        return child;
    }

    private native void initPolymer()
    /*-{
        $wnd.Polymer = {};
        $wnd.Polymer.dom = function(node){
            return node;
        };
    }-*/;

    private native void mockWhenDefined(Element element)
    /*-{
        $wnd.customElements = {
            whenDefined: function() {
                return {
                    then: function (callback) {
                        element.callback = callback;
                    }
                }
            }
        };
    }-*/;

    private native void runWhenDefined(Element element)
    /*-{
        element.callback();
    }-*/;

    private native void defineProperty(Element element, String property)
    /*-{
        element["constructor"] ={};
        element["constructor"]["properties"] ={};
        element["constructor"]["properties"][property] ={};
        element["constructor"]["properties"][property]["value"] ={};
    }-*/;
}
