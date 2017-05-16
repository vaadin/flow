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
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;

public class GwtElementUtilsTest extends ClientEngineTestBase {

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
                int requestedId, int assignedId, String tagName, String id) {
            sentExistingElementParent = parent;
            sentExistingElementRequestedId = requestedId;
            sentExistingElementAssignedId = assignedId;
            sentExistingElementTagName = tagName;
            sentExistingElementId = id;
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

        ElementUtils.attachExistingElement(node, null, "button", requestedId);

        assertRpcToServerArguments(requestedId, child3.getTagName(), 1);
    }

    public void testAttachExistingElement_siblingIdProvided() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        Element child2 = addChildElement("a");
        addChild(child2, 1);

        addChildElement("span");
        Element child4 = addChildElement("button");

        ElementUtils.attachExistingElement(node, child2, "button", requestedId);

        assertRpcToServerArguments(requestedId, child4.getTagName(), 2);
    }

    public void testAttachExistingElement_elementHasServersideCounterpart() {
        Element child1 = addChildElement("span");

        StateNode childNode = addChild(child1, 0);

        addChildElement("button");

        ElementUtils.attachExistingElement(node, null, "span", requestedId);

        assertRpcToServerArguments(childNode.getId(), child1.getTagName(), 0);
    }

    public void testAttachExistingElement_elementIsAlreadyAssociated() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        Element child2 = addChildElement("button");

        int associatedId = 13;
        map.add(associatedId, child2);

        ElementUtils.attachExistingElement(node, null, "button", requestedId);

        assertRpcToServerArguments(associatedId, child2.getTagName(), 1);
    }

    public void testAttachExistingElement_noRequestedElement() {
        Element child1 = addChildElement("span");

        addChild(child1, 0);

        ElementUtils.attachExistingElement(node, null, "button", requestedId);

        assertRpcToServerArguments(-1, "button", -1);
    }

    public void testAttachExistingElementById_elementExistsInDom() {
        Element shadowRoot = setupShadowRoot();

        String id = "identifier";

        Element child = Browser.getDocument().createElement("div");
        child.setAttribute("id", id);
        shadowRoot.appendChild(child);

        ElementUtils.attachExistingElementById(node,
                "div", requestedId, id);

        assertRpcToServerArguments(requestedId, child.getTagName(), id);
    }

    public void testAttachExistingElementById_elementMissingInDom() {
        setupShadowRoot();

        ElementUtils.attachExistingElementById(node,
                "div", requestedId, "not_found");

        assertRpcToServerArguments(-1, "div", "not_found");
    }

    public void testAttachExistingElementById_elementIsAlreadyAssociated() {
        Element shadowRoot = setupShadowRoot();

        String id = "identifier";

        Element child = Browser.getDocument().createElement("div");
        child.setAttribute("id", id);
        shadowRoot.appendChild(child);


        NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        StateNode shadowRootNode = (StateNode) map
                .getProperty(NodeFeatures.SHADOW_ROOT).getValue();
        NodeList list = shadowRootNode
                .getList(NodeFeatures.ELEMENT_CHILDREN);

        StateNode elementNode = new StateNode(99, tree);
        elementNode.setDomNode(child);

        list.add(0, elementNode);

        ElementUtils.attachExistingElementById(node,
                "div", requestedId, id);

        assertRpcToServerArguments(99, child.getTagName(), id);
    }

    // This test emulates FireFox that doesn't hide element children under shadowRoot
    public void testAttachExistingElementById_elementOutsideShadowRoot() {
        Browser.getDocument().getBody().appendChild(element);
        setupShadowRoot();

        String id = "identifier";

        Element child = Browser.getDocument().createElement("div");
        child.setAttribute("id", id);
        element.appendChild(child);

        ElementUtils.attachExistingElementById(node,
                "div", requestedId, id);

        assertRpcToServerArguments(requestedId, child.getTagName(), id);
    }

    // This test emulates Edge that doesn't support requesting getElementById from shadowRoot
    public void testAttachExistingElementById_noByIdMethodInShadowRoot() {
        Browser.getDocument().getBody().appendChild(element);
        Element shadowRoot = addShadowRootWithoutGetElementById(element);

        NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        map.getProperty(NodeFeatures.SHADOW_ROOT).setValue(new StateNode(34, tree));

        String id = "identifier";

        Element child = Browser.getDocument().createElement("div");
        child.setAttribute("id", id);
        element.appendChild(child);

        ElementUtils.attachExistingElementById(node,
                "div", requestedId, id);

        assertRpcToServerArguments(requestedId, child.getTagName(), id);
    }

    private Element setupShadowRoot() {
        Element shadowRoot = addShadowRoot(element);

        NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        map.getProperty(NodeFeatures.SHADOW_ROOT).setValue(new StateNode(34, tree));

        return shadowRoot;
    }

    private native Element addShadowRoot(Element element) /*-{
        element.shadowRoot = $doc.createElement("div");
        element.shadowRoot.getElementById = function(id) {
            var children = element.shadowRoot.children;
            for (var i = 0; i < children.length; i++) {
                var child = children[i];
                if(child.getAttribute("id") === id) {
                    return child;
                }
            }
        }

        return element.shadowRoot;
    }-*/;

    private native Element addShadowRootWithoutGetElementById(Element element) /*-{
        element.shadowRoot = $doc.createElement("div");
        return element.shadowRoot;
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
        assertEquals(tagName, tree.sentExistingElementTagName);
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
}
