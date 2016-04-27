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
package com.vaadin.hummingbird.nodefeature;

import com.vaadin.hummingbird.StateNode;

/**
 * Data model feature for template data binding.
 * 
 * @author Vaadin Ltd
 *
 */
public class ModelMap extends NodeMap {

    /**
     * Creates an instance of this node feature.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public ModelMap(StateNode node) {
        super(node);
    }

    /**
     * Sets the {@code value} for the specified {@code key}.
     *
     * @param key
     *            key with which the specified value is to be associated, not
     *            {@code null}
     * @param value
     *            value to be associated with the specified key
     */
    public void setValue(String key, String value) {
        assert key != null;
        put(key, value);
    }

    /**
     * Gets the value corresponding to the given key.
     * 
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the key; <code>null</code> if there is
     *         no value stored, or if <code>null</code> is stored as a value
     */
    public String getValue(String key) {
        return (String) get(key);
    }
}
