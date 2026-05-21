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

import jsinterop.annotations.JsType;

import com.vaadin.client.Registry;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.dom.DomNode;
import com.vaadin.client.flow.nodefeature.MapProperty;

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

    /**
     * Unregisters all nodes except root from this tree, and clears the root's
     * features.
     */
    public native void prepareForResync();

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
