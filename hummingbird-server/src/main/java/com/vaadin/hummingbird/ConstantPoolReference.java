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

import java.io.Serializable;

import com.vaadin.server.SerializableFunction;

import elemental.json.JsonValue;

/**
 * A reference to a value that should be stored in the {@link ConstantPool}
 * shared between the client and the server.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the type of the referenced value
 */
public class ConstantPoolReference<T extends Serializable>
        implements Serializable {
    private T value;
    private SerializableFunction<T, JsonValue> jsonConverter;

    /**
     * Creates a new constant pool reference from a value and a JSON serializer.
     *
     * @param value
     *            the referenced value, not <code>null</code>
     * @param jsonConverter
     *            a callback for serializing the value to JSON, not
     *            <code>null</code>
     */
    public ConstantPoolReference(T value,
            SerializableFunction<T, JsonValue> jsonConverter) {
        assert value != null;
        assert jsonConverter != null;

        this.value = value;
        this.jsonConverter = jsonConverter;
    }

    /**
     * Gets the referenced value. The value should be considered immutable.
     *
     * @return the referenced value, not <code>null</code>
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets a JSON representation of the referenced value.
     *
     * @return a JSON representation of the referenced value, not
     *         <code>null</code>
     */
    public JsonValue getJson() {
        JsonValue json = jsonConverter.apply(value);
        assert json != null;
        return json;
    }
}
