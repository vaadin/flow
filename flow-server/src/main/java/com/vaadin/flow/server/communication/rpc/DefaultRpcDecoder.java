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

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JsonCodec;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * Decodes the standard basic types from their JSON representation.
 * <p>
 * Delegates to the standard JSON deserializer method
 * {@link JacksonCodec#decodeAs(JsonNode, Class) <p> For internal use only. May
 * be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @see JacksonCodec#decodeAs(JsonNode, Class)
 * @since 1.0
 *
 */
public class DefaultRpcDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonNode value, Class<?> type) {
        return JacksonCodec.canEncodeWithoutTypeInfo(type);
    }

    @Override
    public <T> T decode(JsonNode value, Class<T> type)
            throws RpcDecodeException {
        return JacksonCodec.decodeAs(value, type);
    }

}
