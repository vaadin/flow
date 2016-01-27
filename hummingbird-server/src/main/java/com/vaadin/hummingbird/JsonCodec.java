/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON.
 *
 * @since
 * @author Vaadin Ltd
 */
public class JsonCodec {
    private JsonCodec() {
        // Don't create instances
    }

    /**
     * Helper for encoding any primitive value as JSON.
     *
     * @param value
     *            the value to encode
     * @return the encode JSON value
     */
    public static JsonValue encodePrimitiveValue(Object value) {
        if (value instanceof String) {
            return Json.create((String) value);
        } else if (value instanceof Number) {
            return Json.create(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            return Json.create(((Boolean) value).booleanValue());
        } else if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value == null) {
            return Json.createNull();
        } else {
            throw new IllegalArgumentException(
                    "Can't encode" + value.getClass() + " to json");
        }
    }

}
