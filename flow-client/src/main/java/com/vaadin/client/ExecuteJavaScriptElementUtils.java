/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.model.UpdatableModelProperties;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utility class which handles javascript execution context (see
 * ExecuteJavaScriptProcessor#getContextExecutionObject()).
 *
 * @see ExecuteJavaScriptProcessor
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public final class ExecuteJavaScriptElementUtils {

    private ExecuteJavaScriptElementUtils() {
    }

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
        JsMap<Node, Integer> indices = new JsMap<>();
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
                Integer index = indices.get(domNode);
                if (index != null && index < elementIndex) {
                    childIndex++;
                }

                if (domNode.equals(existingElement)) {
                    existingId = stateNode.getId();
                    break;
                }
            }

            existingId = getExistingIdOrUpdate(parent, id, existingElement,
                    existingId);

            parent.getTree().sendExistingElementAttachToServer(parent, id,
                    existingId, existingElement.getTagName(), childIndex);
        }
    }

    private static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }

    /**
     * Populate model {@code properties}: add them into
     * {@literal NodeFeatures.ELEMENT_PROPERTIES} {@link NodeMap} if they are
     * not defined by the client-side element or send their client-side value to
     * the server otherwise.
     *
     * @param node
     *            the node whose properties should be populated
     * @param properties
     *            array of property names to populate
     */
    public static void populateModelProperties(StateNode node,
            JsArray<String> properties) {
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        if (node.getDomNode() == null) {
            PolymerUtils.invokeWhenDefined(PolymerUtils.getTag(node),
                    () -> Reactive.addPostFlushListener(
                            () -> populateModelProperties(node, properties)));
            return;
        }
        for (int i = 0; i < properties.length(); i++) {
            populateModelProperty(node, map, properties.get(i));
        }
    }

    private static void populateModelProperty(StateNode node, NodeMap map,
            String property) {
        if (!isPropertyDefined(node.getDomNode(), property)) {
            if (!map.hasPropertyValue(property)) {
                map.getProperty(property).setValue(null);
            }
        } else {
            UpdatableModelProperties updatableProperties = node
                    .getNodeData(UpdatableModelProperties.class);
            if (updatableProperties == null
                    || !updatableProperties.isUpdatableProperty(property)) {
                return;
            }
            map.getProperty(property).syncToServer(
                    WidgetUtil.getJsProperty(node.getDomNode(), property));
        }
    }

    /**
     * Register the updatable model properties of the {@code node}.
     * <p>
     * Only updates for the properties from the {@code properties} array will be
     * sent to the server without explicit synchronization. The
     * {@code properties} array includes all properties that are allowed to be
     * updated (including sub properties).
     *
     * @param node
     *            the node whose updatable properties should be registered
     * @param properties
     *            all updatable model properties
     */
    public static void registerUpdatableModelProperties(StateNode node,
            JsArray<String> properties) {
        if (!properties.isEmpty()) {
            UpdatableModelProperties data = new UpdatableModelProperties(
                    properties);
            node.setNodeData(data);
        }
    }

    private static Integer getExistingIdOrUpdate(StateNode parent,
            int serverSideId, Element existingElement, Integer existingId) {
        if (existingId == null) {
            ExistingElementMap map = parent.getTree().getRegistry()
                    .getExistingElementMap();
            Integer fromMap = map.getId(existingElement);
            if (fromMap == null) {
                map.add(serverSideId, existingElement);
                return serverSideId;
            }
            return fromMap;
        }
        return existingId;
    }

    private static native boolean isPropertyDefined(Node node, String property)
    /*-{
        return !!(node["constructor"] && node["constructor"]["properties"] &&
            node["constructor"]["properties"][property]) &&
                 (typeof(node["constructor"]["properties"][property]["value"]) != "undefined");
    }-*/;
}
