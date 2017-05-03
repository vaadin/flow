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

import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.flow.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utility class which handles javascript execution context (see
 * ExecuteJavaScriptProcessor#getContextExecutionObject()).
 * 
 * @see ExecuteJavaScriptProcessor
 * 
 * @author Vaadin Ltd
 *
 */
public class ElementUtils {

    /**
     * Calculate the data required for server side callback to attach existing
     * element and send it to the server.
     * 
     * @param parent
     *            the parent node whose child is requested to attach
     * @param previousSibling
     *            previous sibling element
     * @param tagName
     *            the tag name of the element requested to attach
     * @param id
     *            the identifier of the server side node which is requested to
     *            be a counterpart of the client side element
     */
    public static void attachExistingElement(StateNode parent,
            Element previousSibling, String tagName, int id) {
        Element existingElement = null;
        JsArray<Node> childNodes = DomApi.wrap(parent.getDomNode())
                .getChildNodes();
        JsMap<Node, Integer> indices = JsCollections.map();
        boolean afterSibling = previousSibling == null;
        int elementIndex = -1;
        for (int i = 0; i < childNodes.length(); i++) {
            Node node = childNodes.get(i);
            indices.set(node, i);
            if (node.equals(previousSibling)) {
                afterSibling = true;
            }
            if (afterSibling && hasTag(node, tagName)) {
                existingElement = (Element) node;
                elementIndex = i;
                break;
            }
        }

        if (existingElement == null) {
            // report an error
            parent.getTree().sendExistingElementAttachToServer(parent, id, -1,
                    tagName, -1);
        } else {
            NodeList list = parent.getList(NodeFeatures.ELEMENT_CHILDREN);
            Integer existingId = null;
            int childIndex = 0;
            for (int i = 0; i < list.length(); i++) {
                StateNode stateNode = (StateNode) list.get(i);
                Node domNode = stateNode.getDomNode();
                if (indices.get(domNode) < elementIndex) {
                    childIndex++;
                }

                if (domNode.equals(existingElement)) {
                    existingId = stateNode.getId();
                    break;
                }
            }

            if (existingId == null) {
                ExistingElementMap map = parent.getTree().getRegistry()
                        .getExistingElementMap();
                existingId = map.getId(existingElement);
                if (existingId == null) {
                    existingId = id;
                    map.add(id, existingElement);
                }
            }
            parent.getTree().sendExistingElementAttachToServer(parent, id,
                    existingId, existingElement.getTagName(), childIndex);
        }
    }

    private static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }
}
