/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonCodec;

import elemental.json.JsonValue;

/**
 * Decodes the standard basic types from their JSON representation.
 * <p>
 * Delegates to the standard JSON deserializer method
 * {@link JsonCodec#decodeAs(JsonValue, Class)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see JsonCodec#decodeAs(JsonValue, Class)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class DefaultRpcDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonValue value, Class<?> type) {
        return JsonCodec.canEncodeWithoutTypeInfo(type)
                || JacksonCodec.canEncodeWithoutTypeInfo(type);
    }

    @Override
    public <T> T decode(JsonValue value, Class<T> type)
            throws RpcDecodeException {
        if (type.isAssignableFrom(JsonNode.class)) {
            return JacksonCodec.decodeAs(JacksonUtils.mapElemental(value),
                    type);
        }
        return JsonCodec.decodeAs(value, type);
    }

}
