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

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.ReactiveValue;
import com.vaadin.client.flow.reactive.ReactiveValueChangeListener;

import elemental.events.EventRemover;
import elemental.json.JsonValue;

/**
 * A state node feature that structures data as a map. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/nodefeature/NodeMap.ts}.
 *
 * The feature works as a reactive value with regards to the set of available
 * properties. A {@link Computation} will get a dependency on this feature by
 * iterating the properties.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.nodefeature", name = "NodeMap")
public class NodeMap extends NodeFeature implements ReactiveValue {

    public NodeMap(int id, StateNode node) {
        super(id, node);
    }

    public native MapProperty getProperty(String name);

    public native boolean hasPropertyValue(String name);

    public native void forEachProperty(
            ForEachCallback<String, MapProperty> callback);

    public native JsArray<String> getPropertyNames();

    @Override
    public native JsonValue getDebugJson();

    @Override
    public native JsonValue convert(Function<Object, JsonValue> converter);

    @Override
    public native EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener);

    public native EventRemover addPropertyAddListener(
            MapPropertyAddListener listener);
}
