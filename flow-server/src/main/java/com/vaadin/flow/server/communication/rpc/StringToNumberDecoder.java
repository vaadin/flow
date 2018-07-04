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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Decodes a {@link JsonValue} with {@link JsonType#STRING} type to
 * {@link Number} subclass type.
 * <p>
 * This decoder is applicable to any {@link JsonValue} which is
 * {@link JsonString} and any primitive type wrapper {@link Number} subclass
 * (like {@link Integer}, {@link Double}, {@link Long}, etc.)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StringToNumberDecoder implements RpcDecoder {

    @Override
    public boolean isApplicable(JsonValue value, Class<?> type) {
        return value.getType().equals(JsonType.STRING)
                && Number.class.isAssignableFrom(type)
                && type.getPackage().equals(Integer.class.getPackage());
    }

    @Override
    public <T> T decode(JsonValue value, Class<T> type)
            throws RpcDecodeException {
        String stringValue = value.asString();
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
