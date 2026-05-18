/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jsinterop.annotations.JsFunction;

import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
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

    /**
     * Bridge interface for a JS callback with no arguments. Used to convert a
     * JavaScript function received from the executed expression into a
     * Java-callable target.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface JsCallback {
        void invoke();
    }

    private static final JsMap<StateNode, JsMap<Double, JsCallback>> initializerCleanups = JsCollections
            .map();

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

    /**
     * Stores a cleanup callback for a JS initializer registered through
     * {@link com.vaadin.flow.dom.Element#addJsInitializer}. If a callback was
     * previously stored for the same id, it is invoked before being replaced
     * (defensive against stale state from a discarded DOM). On the first
     * registration for a node, an unregister listener is attached so that all
     * remaining cleanups are drained when the node leaves the tree.
     *
     * @param node
     *            the state node owning the initializer, not {@code null}
     * @param id
     *            the UI-wide initializer id
     * @param cleanup
     *            the JS cleanup function to invoke when disposing, not
     *            {@code null}
     */
    public static void registerInitializer(StateNode node, double id,
            JsCallback cleanup) {
        JsMap<Double, JsCallback> entry = initializerCleanups.get(node);
        if (entry == null) {
            entry = JsCollections.map();
            initializerCleanups.set(node, entry);
            node.addUnregisterListener(event -> drainInitializers(node));
        }
        JsCallback existing = entry.get(id);
        // Install the new cleanup before invoking the previous one so a
        // re-entrant register/dispose call from inside the existing callback
        // sees the new state, not the stale entry.
        entry.set(id, cleanup);
        if (existing != null) {
            invokeSafely(existing);
        }
    }

    /**
     * Disposes a previously registered JS initializer cleanup. No-op if the id
     * is unknown (e.g. the node has already been unregistered).
     *
     * @param node
     *            the state node owning the initializer, not {@code null}
     * @param id
     *            the UI-wide initializer id
     */
    public static void disposeInitializer(StateNode node, double id) {
        JsMap<Double, JsCallback> entry = initializerCleanups.get(node);
        if (entry == null) {
            return;
        }
        JsCallback fn = entry.get(id);
        if (fn == null) {
            return;
        }
        entry.delete(id);
        invokeSafely(fn);
    }

    private static void drainInitializers(StateNode node) {
        JsMap<Double, JsCallback> entry = initializerCleanups.get(node);
        if (entry == null) {
            return;
        }
        initializerCleanups.delete(node);
        entry.forEach((fn, id) -> invokeSafely(fn));
    }

    private static void invokeSafely(JsCallback fn) {
        try {
            fn.invoke();
        } catch (RuntimeException e) {
            Console.error(e.getMessage());
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

    private static boolean isPropertyDefined(Node node, String property) {
        return com.google.gwt.core.client.GWT.isScript()
                && NativeExecuteJavaScriptElementUtils.isPropertyDefined(node,
                        property);
    }
}
