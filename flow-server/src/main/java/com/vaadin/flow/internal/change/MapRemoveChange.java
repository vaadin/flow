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
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Change describing a value removed from a map.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapRemoveChange extends NodeFeatureChange {

    private final String key;

    /**
     * Creates a new remove change.
     *
     * @param map
     *            the changed map
     * @param key
     *            the removed key
     */
    public MapRemoveChange(NodeMap map, String key) {
        super(map);

        this.key = key;
    }

    /**
     * Gets the removed key.
     *
     * @return the removed key
     */
    public String getKey() {
        return key;
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        // Set the type before calling super to make the keys appear in a more
        // logical order
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_REMOVE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_MAP_KEY, key);
    }
}
