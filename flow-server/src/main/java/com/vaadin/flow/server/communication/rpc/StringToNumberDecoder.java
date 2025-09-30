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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;

/**
 * Decodes a {@link JsonNode} with {@link JsonNodeType#STRING} type to
 * {@link Number} subclass type.
 * <p>
 * This decoder is applicable to any {@link JsonNode} which is
 * {@link tools.jackson.databind.node.StringNode} and any primitive type wrapper
 * {@link Number} subclass (like {@link Integer}, {@link Double}, {@link Long},
 * etc.).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StringToNumberDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonNode value, Class<?> type) {
        return value.getNodeType().equals(JsonNodeType.STRING)
                && Number.class.isAssignableFrom(type)
                && type.getPackage().equals(Integer.class.getPackage());
    }

    @Override
    public <T> T decode(JsonNode value, Class<T> type)
            throws RpcDecodeException {
        String stringValue = value.asText();
        try {
            Number number = parseNumber(stringValue);
            if (Number.class.equals(type)) {
                return type.cast(number);
            }
            Field requiredType = type.getField("TYPE");
            Class<?> primitiveRequiredType = (Class<?>) requiredType.get(null);
            Method method = Number.class
                    .getMethod(primitiveRequiredType + "Value");
            T result = type.cast(method.invoke(number));
            if (number.equals(parseNumber(result.toString()))) {
                // check whether the number is the same after the projection
                // (applying the "xxxValue" method)
                return result;
            } else {
                throw new RpcDecodeException(
                        String.format("Can't decode '%s' to type '%s'",
                                stringValue, type.getName()));
            }
        } catch (ParseException exception) {
            throw new RpcDecodeException(exception);
        } catch (NoSuchFieldException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException | InvocationTargetException exception) {
            // can't happen
            throw new RuntimeException(exception);
        }
    }

    private Number parseNumber(String value) throws ParseException {
        return NumberFormat.getInstance(Locale.ENGLISH).parse(value);
    }

}
