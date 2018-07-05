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

package com.vaadin.flow.internal.change;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Change describing a value removed from a map.
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
