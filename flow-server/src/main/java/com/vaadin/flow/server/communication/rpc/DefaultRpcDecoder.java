/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonCodec;

/**
 * Decodes JSON values to Java objects using Jackson deserialization.
 * <p>
 * Supports a wide range of types including basic types, custom bean classes,
 * and generic collections. Delegates to the enhanced JSON deserializer method
 * {@link JacksonCodec#decodeAs(JsonNode, Class)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @see JacksonCodec#decodeAs(JsonNode, Class)
 * @since 1.0
 *
 */
public class DefaultRpcDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonNode value, Class<?> type) {
        // This decoder handles all types that JacksonCodec.decodeAs can
        // process,
        // which includes basic types, custom beans, and collections
        return true;
    }

    @Override
    public <T> T decode(JsonNode value, Class<T> type)
            throws RpcDecodeException {
        return JacksonCodec.decodeAs(value, type);
    }

}
