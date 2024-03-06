/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Map of constant values received from the server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ConstantPool {
    private JsMap<String, JsonValue> constants = JsCollections.map();

    /**
     * Imports new constants into this pool.
     *
     * @param json
     *            a JSON object mapping constant keys to constant values, not
     *            <code>null</code>
     */
    public void importFromJson(JsonObject json) {
        assert json != null;

        for (String key : json.keys()) {
            assert !constants.has(key);
            JsonValue value = json.get(key);

            assert value != null && value.getType() != JsonType.NULL;
            constants.set(key, value);
        }
    }

    /**
     * Checks whether this constant pool contains a value for the given key.
     *
     * @param key
     *            the key to check, not <code>null</code>
     * @return <code>true</code> if there is a constant for the given key;
     *         otherwise <code>false</code>
     */
    public boolean has(String key) {
        assert key != null;
        return constants.has(key);
    }

    /**
     * Gets the constant with a given key.
     *
     * @param key
     *            the key to get a constant for, not <code>null</code>
     * @param <T>
     *            the constant type
     * @return the constant value, or <code>null</code> if there is no constant
     *         with the given key
     */
    @SuppressWarnings("unchecked")
    // Returns any type to make it easier to use constants as JsInterop types
    public <T> T get(String key) {
        assert key != null;
        return (T) constants.get(key);
    }

}
