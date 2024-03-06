/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.change;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Change describing a changed value in a map feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPutChange extends NodeFeatureChange {

    private final String key;
    private final Object value;

    /**
     * Creates a new put change.
     *
     * @param map
     *            the changed map
     * @param key
     *            the key of the changed value
     * @param value
     *            the new value
     */
    public MapPutChange(NodeFeature map, String key, Object value) {
        super(map);

        assert key != null;

        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key of the change.
     *
     * @return the key of the change
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the new value.
     *
     * @return the new value
     */
    public Object getValue() {
        return value;
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        // Set the type and key before calling super to make the keys appear in
        // a more logical order
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_PUT);
        json.put(JsonConstants.CHANGE_MAP_KEY, key);

        super.populateJson(json, constantPool);

        if (value instanceof StateNode) {
            StateNode node = (StateNode) value;
            json.put(JsonConstants.CHANGE_PUT_NODE_VALUE,
                    Json.create(node.getId()));
        } else {
            json.put(JsonConstants.CHANGE_PUT_VALUE,
                    JsonCodec.encodeWithConstantPool(value, constantPool));
        }
    }
}
