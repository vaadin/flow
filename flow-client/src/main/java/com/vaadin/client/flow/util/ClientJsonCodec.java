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
package com.vaadin.client.flow.util;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;

import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/util/ClientJsonCodec.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.util", name = "ClientJsonCodec")
public class ClientJsonCodec {

    private ClientJsonCodec() {
        // Static-only.
    }

    /**
     * Decodes a value as a {@link StateNode} encoded on the server using the
     * server's typed-info encoding if it's possible. Otherwise returns
     * {@code null}.
     */
    public static native StateNode decodeStateNode(StateTree tree,
            JsonValue json);

    /**
     * Decodes a value encoded on the server using the server's typed-info
     * encoding (resolves @v-node references and @v-return channels).
     */
    public static native Object decodeWithTypeInfo(StateTree tree,
            JsonValue json);

    /**
     * Decodes a value encoded on the server using the server's untyped
     * encoding. In compiled JS this is effectively a passthrough since
     * elemental.json accessors collapse to the wrapped primitive.
     */
    public static native Object decodeWithoutTypeInfo(JsonValue json);

    /**
     * Encodes a "primitive" value (String / Number / Boolean / JsonValue /
     * null) for serialization. {@code null} is preserved; {@code undefined}
     * becomes {@code null}.
     */
    public static native JsonValue encodeWithoutTypeInfo(Object value);
}
