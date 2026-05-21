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
package com.vaadin.client.flow;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.binding.ServerEventObject;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.dom.DomNode;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.dom.Node;
import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state tree. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/StateTree.ts}. Server-dispatch
 * methods stay Java-side as {@code @JsOverlay} helpers because they reach into
 * the still-Java Registry / ServerConnector chain.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "StateTree")
public class StateTree {

    public StateTree(Registry registry) {
        // Defined by the TS class constructor.
    }

    public native boolean isUpdateInProgress();

    /**
     * Mark this tree as being updated, and flush any queued initial-property
     * syncs once the update completes. Implementation lives in the TS class.
     */
    public native void setUpdateInProgress(boolean updateInProgress);

    /**
     * Wires the InitialPropertiesHandler into this tree so
     * {@link #setUpdateInProgress(boolean)} can flush queued property syncs.
     * Called once at registry construction.
     */
    public native void setInitialPropertiesHandler(
            com.vaadin.client.InitialPropertiesHandler handler);

    /** Registers a node with this tree. */
    public native void registerNode(StateNode node);

    /** Unregisters a node from this tree. */
    public native void unregisterNode(StateNode node);

    public native boolean isResync();

    public native void setResync(boolean resync);

    public native StateNode getNode(int id);

    public native StateNode getRootNode();

    public native StateNode getStateNodeForDomNode(DomNode domNode);

    public native boolean isVisible(StateNode node);

    public native boolean isActive(StateNode node);

    public native String getFeatureDebugName(int id);

    public native Registry getRegistry();

    @JsMethod(name = "forEachNode")
    native void forEachNodeImpl(JsForEachNodeCallback callback);

    @jsinterop.annotations.JsFunction
    @FunctionalInterface
    @SuppressWarnings("unusable-by-js")
    interface JsForEachNodeCallback {
        void accept(StateNode node);
    }

    /**
     * Unregisters all nodes except root from this tree, and clears the root's
     * features.
     */
    @JsOverlay
    public final void prepareForResync() {
        getRootNode().getList(NodeFeatures.VIRTUAL_CHILDREN)
                .forEach(sn -> clearLists((StateNode) sn));
        clearLists(getRootNode());

        StateNode root = getRootNode();
        forEachNodeImpl(node -> {
            if (node != root) {
                final Node dom = node.getDomNode();
                if (dom != null
                        && ServerEventObject.getIfPresent(dom) != null) {
                    ServerEventObject.getIfPresent(dom).rejectPromises();
                }
                unregisterNode(node);
                node.setParent(null);
            }
        });
        setResync(true);
    }

    @JsOverlay
    private void clearLists(StateNode stateNode) {
        stateNode.forEachFeature((feature, featureId) -> {
            if (feature instanceof NodeList) {
                final NodeList nodeList = (NodeList) feature;
                if (featureId.intValue() == NodeFeatures.ELEMENT_CHILDREN) {
                    nodeList.splice(0, nodeList.length());
                } else {
                    nodeList.clear();
                }
            }
        });
    }

    @JsOverlay
    private boolean assertValidNode(StateNode node) {
        assert node != null : "Node is null";
        assert node.getTree() == this : "Node is not created for this tree";
        assert node == getNode(node.getId())
                : "Node id is not registered with this tree";
        return true;
    }

    @JsOverlay
    private boolean isValidNode(StateNode node) {
        boolean isValid = true;
        if (node == null) {
            Console.warn("Node is null");
            isValid = false;
        } else if (!node.getTree().equals(this)) {
            Console.warn("Node is not created for this tree");
            isValid = false;
        } else if (!node.equals(getNode(node.getId()))) {
            Console.warn("Node id is not registered with this tree");
            isValid = false;
        }
        return isValid;
    }

    /** Wires the server connector for the send*ToServer dispatchers. */
    public native void setServerConnector(
            com.vaadin.client.communication.ServerConnector connector);

    public native void sendEventToServer(StateNode node, String eventType,
            JsonObject eventData);

    public native void sendNodePropertySyncToServer(MapProperty property);

    public native void sendTemplateEventToServer(StateNode node,
            String methodName, JsArray<?> argsArray, int promiseId);

    public native void sendExistingElementAttachToServer(StateNode parent,
            int requestedId, int assignedId, String tagName, int index);

    public native void sendExistingElementWithIdAttachToServer(StateNode parent,
            int requestedId, int assignedId, String id);
}
