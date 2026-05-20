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

import elemental.json.JsonObject;

/**
 * Map of constant values received from the server. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/ConstantPool.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "ConstantPool")
public class ConstantPool {

    public ConstantPool() {
        // Defined by the TS class constructor.
    }

    /** Imports new constants into this pool. */
    public native void importFromJson(JsonObject json);

    /** Checks whether this constant pool contains a value for the given key. */
    public native boolean has(String key);

    /**
     * Gets the constant with a given key.
     * <p>
     * Returns any type to make it easier to use constants as JsInterop types.
     */
    public native <T> T get(String key);
}
