/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Decodes a {@link JsonValue} with {@link JsonType#STRING} type to {@link Enum}
 * subclass type.
 * <p>
 * This decoder is applicable to any {@link JsonValue} which is
 * {@link JsonString} and any {@link Enum} sublcass
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
