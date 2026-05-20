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

import elemental.json.JsonValue;

/**
 * Holder of the actual data in a state node. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/nodefeature/NodeFeature.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.nodefeature", name = "NodeFeature")
public abstract class NodeFeature {

    public NodeFeature(int id, StateNode node) {
        // Defined by the TS class constructor.
    }

    public native int getId();

    public native StateNode getNode();

    public abstract JsonValue getDebugJson();

    public abstract JsonValue convert(Function<Object, JsonValue> converter);
}
