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

import java.io.Serializable;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;

/**
 * Map for model values used in data binding in templates.
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
    public void setValue(String key, Serializable value) {
        assert key != null;
        if (key.contains(".")) {
            throw new IllegalArgumentException(
                    "Model map key may not contain dots");
        }

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
    public Serializable getValue(String key) {
        return (Serializable) get(key);
    }

    /**
     * Checks whether a value is stored for the given key.
     * <p>
     * If method {@link #setValue(String, Serializable)} has never been called
     * for the {@code key} then {@code false} is returned. Otherwise (even if it
     * has been called with {@code null} as a value) it returns {@code true}. It
     * means that {@link #getValue(String)} may return {@code null} at the same
     * time when {@link #hasValue(String)} returns {@code true}.
     *
     * @see #setValue(String, Serializable)
     *
     * @param key
     *            the key to check a value for
     * @return <code>true</code> if there is a value stored; <code>false</code>
     *         if no value is stored
     */
    public boolean hasValue(String key) {
        return super.contains(key);
    }

    @Override
    public Set<String> keySet() {
        return super.keySet();
    }

    /**
     * Gets the model map for the given node.
     * <p>
     * Throws an exception if the node does not have a model map.
     *
     * @param node
     *            the node which has a model map
     * @return the model map for the node
     */
    public static ModelMap get(StateNode node) {
        assert node != null;
        return node.getFeature(ModelMap.class);
    }

}
