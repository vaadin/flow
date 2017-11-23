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

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.HTMLCollection;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Utility class which handles javascript execution context (see
 * ExecuteJavaScriptProcessor#getContextExecutionObject()).
 *
 * @see ExecuteJavaScriptProcessor
 *
 * @author Vaadin Ltd
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
     * Find element for given id and collect data required for server side
     * callback to attach existing element and send it to the server.
     *
     * @param parent
     *            the parent node containing the shadow root containing the
     *            element requested to attach
     * @param tagName
     *            the tag name of the element requested to attach
     * @param serverSideId
     *            the identifier of the server side node which is requested to
     *            be a counterpart of the client side element
     * @param id
     *            the id attribute of the element to wire to
     */
    public static void attachExistingElementById(StateNode parent,
            String tagName, int serverSideId, String id) {
        if (parent.getDomNode() == null) {
            Reactive.addPostFlushListener(() -> Scheduler.get()
                    .scheduleDeferred(() -> attachExistingElementById(parent,
                            tagName, serverSideId, id)));
        } else if (getDomRoot(parent.getDomNode()) == null) {
            invokeWhenDefined(parent.getDomNode(),
                    () -> attachExistingElementById(parent, tagName,
                            serverSideId, id));
            return;
        } else {
            Element existingElement = getDomElementById(
                    (Element) parent.getDomNode(), id);

            respondExistingElement(parent, tagName, serverSideId, id,
                    existingElement);
        }
    }

    /**
     * Find element by the given {@code path} in the {@code parent} and collect
     * data required for server side callback to attach existing element and
     * send it to the server.
     *
     * @param parent
     *            the parent node containing the shadow root containing the
     *            element requested to attach
     * @param tagName
     *            the tag name of the element requested to attach
     * @param serverSideId
     *            the identifier of the server side node which is requested to
     *            be a counterpart of the client side element
     * @param path
     *            the path from the {@code parent} template element to the
     *            element to wire to (consist of indices)
     */
    public static void attachCustomElement(StateNode parent, String tagName,
            int serverSideId, JsonArray path) {
        if (getDomRoot(parent.getDomNode()) == null) {
            invokeWhenDefined(parent.getDomNode(),
                    () -> attachCustomElement(parent, tagName, serverSideId,
                            path));
            return;
        }
        Element customElement = getCustomElement(
                getDomRoot(parent.getDomNode()), path);
        if (customElement != null
                && !tagName.equalsIgnoreCase(customElement.getTagName())) {
            Console.warn("Custom element addressed by the path '" + path
                    + "' has wrong tag name '" + customElement.getTagName()
                    + "', required tag '" + tagName + "'");
        }
        respondExistingElement(parent, tagName, serverSideId, null,
                customElement);

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
        for (int i = 0; i < properties.length(); i++) {
            String property = properties.get(i);
            if (!isPropertyDefined(node.getDomNode(), property)) {
                map.getProperty(property).setValue(null);
            } else {
                map.getProperty(property).syncToServer(
                        WidgetUtil.getJsProperty(node.getDomNode(), property));
            }
        }
    }

    private static void respondExistingElement(StateNode parent, String tagName,
            int serverSideId, String id, Element existingElement) {
        if (existingElement != null && hasTag(existingElement, tagName)) {
            NodeMap map = parent.getMap(NodeFeatures.SHADOW_ROOT_DATA);
            StateNode shadowRootNode = (StateNode) map
                    .getProperty(NodeProperties.SHADOW_ROOT).getValue();
            NodeList list = shadowRootNode
                    .getList(NodeFeatures.ELEMENT_CHILDREN);
            Integer existingId = null;

            for (int i = 0; i < list.length(); i++) {
                StateNode stateNode = (StateNode) list.get(i);
                Node domNode = stateNode.getDomNode();

                if (domNode.equals(existingElement)) {
                    existingId = stateNode.getId();
                    break;
                }
            }

            existingId = getExistingIdOrUpdate(shadowRootNode, serverSideId,
                    existingElement, existingId);

            // Return this as attach to parent which will delegate it to the
            // underlying shadowRoot as a virtual child.
            parent.getTree().sendExistingElementWithIdAttachToServer(parent,
                    serverSideId, existingId, existingElement.getTagName(), id);
        } else {
            parent.getTree().sendExistingElementWithIdAttachToServer(parent,
                    serverSideId, -1, tagName, id);
        }
    }

    private static Element getCustomElement(Node root, JsonArray path) {
        Node current = root;
        for (int i = 0; i < path.length(); i++) {
            JsonValue value = path.get(i);
            current = getChildIgnoringStyles(current, (int) value.asNumber());
        }
        if (current instanceof Element) {
            return (Element) current;
        } else if (current == null) {
            Console.warn(
                    "There is no element addressed by the path '" + path + "'");
        } else {
            Console.warn("The node addressed by path " + path
                    + " is not an Element");
        }
        return null;
    }

    private static Node getChildIgnoringStyles(Node parent, int index) {
        HTMLCollection children = DomApi.wrap(parent).getChildren();
        int filteredIndex = -1;
        for (int i = 0; i < children.getLength(); i++) {
            Node next = children.item(i);
            assert next instanceof Element : "Unexpected element type in the collection of children. "
                    + "DomElement::getChildren is supposed to return Element chidren only, but got "
                    + next.getClass();
            Element element = (Element) next;
            if (!"style".equalsIgnoreCase(element.getTagName())) {
                filteredIndex++;
            }
            if (filteredIndex == index) {
                return next;
            }
        }
        return null;
    }

    private static native Element getDomElementById(Element shadowRootParent,
            String id)
    /*-{
        return shadowRootParent.$[id];
    }-*/;

    private static native Element getDomRoot(Node templateElement)
    /*-{
        return templateElement.root;
    }-*/;

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

    private static native void invokeWhenDefined(Node node, Runnable runnable)
    /*-{
        $wnd.customElements.whenDefined(node.localName).then(
            function () {
                runnable.@java.lang.Runnable::run(*)();
            });
    }-*/;

    private static native boolean isPropertyDefined(Node node, String property)
    /*-{
        return node["constructor"] && node["constructor"]["properties"] &&
            node["constructor"]["properties"][property] && node["constructor"]["properties"][property]["value"];
    }-*/;
}
