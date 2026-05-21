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
package com.vaadin.client.flow.model;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;

/**
 * Storage class for the set of updatable model properties on a
 * {@link StateNode}, consulted by {@link SimpleElementBindingStrategy} when
 * deciding whether to push a polymer property update to the server.
 *
 * <p>
 * Pure {@code @JsType(isNative = true)} binding to the TypeScript
 * implementation at
 * {@code src/main/frontend/internal/client/flow/model/UpdatableModelProperties.ts}.
 * Acceptable here because the class is only consumed at runtime from
 * {@code ExecuteJavaScriptElementUtils}; no JUnit test instantiates it.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.model", name = "UpdatableModelProperties")
public class UpdatableModelProperties {

    /**
     * Explicit node-data key used to store an {@code UpdatableModelProperties}
     * instance via {@link StateNode#setNodeData(String, Object)}.
     * Cross-language callers (Java + TS) must use this exact string so they
     * reach the same slot.
     */
    @jsinterop.annotations.JsOverlay
    public static final String NODE_DATA_KEY = "UpdatableModelProperties";

    public UpdatableModelProperties(JsArray<String> properties) {
        // Constructor defined by the TS class.
    }

    public native boolean isUpdatableProperty(String property);
}
