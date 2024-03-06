/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

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
        return JsonCodec.canEncodeWithoutTypeInfo(type);
    }

    @Override
    public <T> T decode(JsonValue value, Class<T> type)
            throws RpcDecodeException {
        return JsonCodec.decodeAs(value, type);
    }

}
