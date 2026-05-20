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
package com.vaadin.client.flow.nodefeature;

import java.util.function.Function;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.ReactiveValue;
import com.vaadin.client.flow.reactive.ReactiveValueChangeListener;

import elemental.events.EventRemover;
import elemental.json.JsonValue;

/**
 * A state node feature that structures data as a list. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/nodefeature/NodeList.ts}.
 *
 * The list works as a reactive value with regards to its structure. A
 * {@link Computation} will get a dependency on this list for any read operation
 * that depends on the list structure.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.nodefeature", name = "NodeList")
public class NodeList extends NodeFeature implements ReactiveValue {

    public NodeList(int id, StateNode node) {
        super(id, node);
    }

    public native int length();

    public native Object get(int index);

    public native void set(int index, Object value);

    public native void add(int index, Object item);

    @JsMethod(name = "spliceRemove")
    public native void splice(int index, int remove);

    public native void clear();

    @JsMethod(name = "spliceArray")
    public final native void splice(int index, int remove, JsArray<?> add);

    public native boolean hasBeenCleared();

    @Override
    public native JsonValue getDebugJson();

    @Override
    public native JsonValue convert(Function<Object, JsonValue> converter);

    public native EventRemover addSpliceListener(ListSpliceListener listener);

    @Override
    public native EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener);

    public native void forEach(ForEachCallback<Object> callback);
}
