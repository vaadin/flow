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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.ListSpliceEvent;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.client.hummingbird.template.TemplateElementBinder;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.EventRemover;

/**
 * Entry point for creating DOM nodes bound to state nodes.
 *
 * @author Vaadin Ltd
 */
public class ElementBinder {
    /**
     * Creates and binds a DOM node for the given state node. For state nodes
     * based on templates, the root element of the template is returned.
     *
     * @param stateNode
     *            the state node for which to create a DOM node, not
     *            <code>null</code>
     * @return the DOM node, not <code>null</code>
     */
    public static Node createAndBind(StateNode stateNode) {
        assert stateNode != null;

        Node node;
        if (stateNode.hasFeature(NodeFeatures.TEXT_NODE)) {
            node = TextElementBinder.createAndBind(stateNode);
        } else if (stateNode.hasFeature(NodeFeatures.TEMPLATE)) {
            node = TemplateElementBinder.createAndBind(stateNode);
        } else if (stateNode.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            node = BasicElementBinder.createAndBind(stateNode);
        } else {
            throw new IllegalArgumentException(
                    "State node has no suitable feature");
        }

        assert node != null;

        stateNode.setDomNode(node);

        return node;
    }

    public static EventRemover bindChildren(Element parent, StateNode node,
            int featureId) {
        NodeList children = node.getList(featureId);

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            Node child = ElementBinder.createAndBind(childNode);

            parent.appendChild(child);
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> handleChildrenSplice(parent, e));
        });
    }

    private static void handleChildrenSplice(Element element,
            ListSpliceEvent event) {
        JsArray<?> remove = event.getRemove();
        for (int i = 0; i < remove.length(); i++) {
            StateNode childNode = (StateNode) remove.get(i);
            Node child = childNode.getDomNode();

            assert child != null : "Can't find element to remove";

            assert child
                    .getParentElement() == element : "Invalid element parent";

            element.removeChild(child);
        }

        JsArray<?> add = event.getAdd();
        if (add.length() != 0) {
            int insertIndex = event.getIndex();
            elemental.dom.NodeList childNodes = element.getChildNodes();

            Node beforeRef;
            if (insertIndex < childNodes.length()) {
                // Insert before the node current at the target index
                beforeRef = childNodes.item(insertIndex);
            } else {
                // Insert at the end
                beforeRef = null;
            }

            for (int i = 0; i < add.length(); i++) {
                Object newChildObject = add.get(i);
                Node childNode = ElementBinder
                        .createAndBind((StateNode) newChildObject);

                element.insertBefore(childNode, beforeRef);

                beforeRef = childNode.getNextSibling();
            }
        }
    }
}
