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

import java.util.function.Function;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;

import elemental.dom.Node;
import elemental.events.EventRemover;
import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state node. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/StateNode.ts}.
 *
 * <p>
 * {@code setNodeData} / {@code getNodeData} / {@code clearNodeData} are keyed
 * by {@code Class<?>.getName()} on the Java {@code @JsOverlay} side and by the
 * resulting string on the TS side. (Collision-free as long as no two stored
 * payload types compile to the same {@code .class.getName()} — which is the
 * pre-migration invariant.)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "StateNode")
public class StateNode {

    /** Predicate type for DOM-node-set listeners. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface DomNodeSetListener {
        Boolean apply(StateNode node);
    }

    public StateNode(int id, StateTree tree) {
        // Defined by the TS class constructor.
    }

    public native StateTree getTree();

    public native int getId();

    public native NodeList getList(int id);

    public native NodeMap getMap(int id);

    public native boolean hasFeature(int id);

    public native void forEachFeature(
            ForEachCallback<Double, NodeFeature> callback);

    public native JsonObject getDebugJson();

    public native boolean isUnregistered();

    public native void unregister();

    public native EventRemover addUnregisterListener(
            NodeUnregisterListener listener);

    public native Node getDomNode();

    public native void setDomNode(Node node);

    public native EventRemover addDomNodeSetListener(
            Function<StateNode, Boolean> listener);

    public native StateNode getParent();

    public native void setParent(StateNode parent);

    @JsMethod(name = "setNodeData")
    native void setNodeDataImpl(String key, Object object);

    @JsMethod(name = "getNodeData")
    native Object getNodeDataImpl(String key);

    @JsMethod(name = "clearNodeData")
    native void clearNodeDataImpl(String key);

    /** Stores the given object under its class name as key. */
    @JsOverlay
    public final <T> void setNodeData(T object) {
        setNodeDataImpl(object.getClass().getName(), object);
    }

    /** Looks up a stored object by its class. */
    @SuppressWarnings("unchecked")
    @JsOverlay
    public final <T> T getNodeData(Class<T> clazz) {
        return (T) getNodeDataImpl(clazz.getName());
    }

    /** Removes the stored object that matches the given object's class. */
    @JsOverlay
    public final <T> void clearNodeData(T object) {
        clearNodeDataImpl(object.getClass().getName());
    }
}
