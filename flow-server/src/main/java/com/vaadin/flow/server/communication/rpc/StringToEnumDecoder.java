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
import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * Decodes a {@link JsonNode} with {@link JsonNodeType#STRING} type to
 * {@link Enum} subclass type.
 * <p>
 * This decoder is applicable to any {@link JsonNode} which is
 * {@link com.fasterxml.jackson.databind.node.TextNode} and any {@link Enum}
 * sublcass
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StringToEnumDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonNode value, Class<?> type) {
        return value.getNodeType().equals(JsonNodeType.STRING) && type.isEnum();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> T decode(JsonNode value, Class<T> type)
            throws RpcDecodeException {
        String stringValue = value.textValue();
        Enum<?> result = Enum.valueOf((Class<? extends Enum>) type,
                stringValue);
        return type.cast(result);
    }

}
