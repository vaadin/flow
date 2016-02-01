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

package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Change describing a value removed from a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapRemoveChange extends NamespaceChange {

    private final String key;

    /**
     * Creates a new remove change.
     *
     * @param namespace
     *            the changed namespace
     * @param key
     *            the removed key
     */
    public MapRemoveChange(MapNamespace namespace, String key) {
        super(namespace);

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
    protected void populateJson(JsonObject json) {
        // Set the type before calling super to make the keys appear in a more
        // logical order
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_REMOVE);

        super.populateJson(json);

        json.put(JsonConstants.CHANGE_MAP_KEY, key);
    }
}
