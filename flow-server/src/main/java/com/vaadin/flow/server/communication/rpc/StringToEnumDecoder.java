/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.rpc;

import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Decodes a {@link JsonValue} with {@link JsonType#STRING} type to {@link Enum}
 * subclass type.
 * <p>
 * This decoder is applicable to any {@link JsonValue} which is
 * {@link JsonString} and any {@link Enum} sublcass
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StringToEnumDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonValue value, Class<?> type) {
        return value.getType().equals(JsonType.STRING) && type.isEnum();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> T decode(JsonValue value, Class<T> type)
            throws RpcDecodeException {
        String stringValue = value.asString();
        Enum<?> result = Enum.valueOf((Class<? extends Enum>) type,
                stringValue);
        return type.cast(result);
    }

}
