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
import com.vaadin.flow.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;

public class GwtElementUtilsTest extends ClientEngineTestBase {

    private StateNode node;

    private Element element;

    private ExistingElementStateTree tree;

    private Registry registry;

    private ExistingElementMap map;

    private int nextId = 10;

    private int requestedId = 5;

    private static class ExistingElementStateTree extends StateTree {

        private StateNode sentExistingElementParent;

        private int sentExistingElementRequestedId;

        private int sentExistingElementAssignedId;

        private String sentExistingElementTagName;

        private int sentExistingElementIndex;

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
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        initPolymer();

        map = new ExistingElementMap();

        registry = new Registry() {
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

    private void assertRpcToServerArguments(int associatedId, String tagName,
            int index) {
        assertEquals(node.getId(), tree.sentExistingElementParent.getId());
        assertEquals(requestedId, tree.sentExistingElementRequestedId);
        assertEquals(associatedId, tree.sentExistingElementAssignedId);
        assertEquals(tagName, tree.sentExistingElementTagName);
        assertEquals(index, tree.sentExistingElementIndex);
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
