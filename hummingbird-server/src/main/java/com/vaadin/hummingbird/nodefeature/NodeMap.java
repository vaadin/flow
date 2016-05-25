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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.MapRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.shared.util.UniqueSerializable;

/**
 * A state node feature that structures data as a map.
 *
 * @author Vaadin Ltd
 */
public abstract class NodeMap extends NodeFeature {
    private static final Serializable REMOVED_MARKER = new UniqueSerializable() {
    };

    private HashMap<String, Serializable> values = new HashMap<>();

    /**
     * Creates a new map feature for the given node.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public NodeMap(StateNode node) {
        super(node);
    }

    /**
     * Stores a value with the given key, replacing any value previously stored
     * with the same key.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to store
     */
    protected void put(String key, Serializable value) {
        doPut(key, value, true);
    }

    /**
     * Stores a value with the given key, replacing any value previously stored
     * with the same key.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to store
     * @param emitChange
     *            true to create a change event for the client side
     */
    protected void put(String key, Serializable value, boolean emitChange) {
        doPut(key, value, emitChange);
    }

    // Internal method to avoid exposing non-serializable setter
    private void doPut(String key, Serializable value, boolean emitChange) {
        if (emitChange) {
            setChanged(key);
        } else {
            setUnChanged(key);
        }
        Object oldValue = values.put(key, value);

        detatchPotentialChild(oldValue);

        attachPotentialChild(value);
    }

    /**
     * Gets the value corresponding to the given key.
     *
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the key; <code>null</code> if there is
     *         no value stored, or if <code>null</code> is stored as a value
     */
    protected Object get(String key) {
        setAccessed(key);
        return values.get(key);
    }

    /**
     * Gets the value corresponding to the given key, or the given default value
     * if no value is stored for the given key or the value is null.
     *
     * @param key
     *            the key to get a value for
     * @param defaultValue
     *            the value to return if no value is stored for the given key
     * @return the value corresponding to the key or the given
     *         {@code defaultValue} if no value is stored for the key or the
     *         stored value is null
     */
    protected String getOrDefault(String key, String defaultValue) {
        if (contains(key)) {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            return (String) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the value corresponding to the given key, or the given default value
     * if no value is stored for the given key or the value is null.
     *
     * @param key
     *            the key to get a value for
     * @param defaultValue
     *            the value to return if no value is stored for the given key
     * @return the value corresponding to the key or the given
     *         {@code defaultValue} if no value is stored for the key or the
     *         stored value is null
     */
    protected int getOrDefault(String key, int defaultValue) {
        if (contains(key)) {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            return (Integer) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the value corresponding to the given key, or the given default value
     * if no value is stored for the given key or the value is null.
     *
     * @param key
     *            the key to get a value for
     * @param defaultValue
     *            the value to return if no value is stored for the given key
     * @return the value corresponding to the key or the given
     *         {@code defaultValue} if no value is stored for the key or the
     *         stored value is null
     */
    protected boolean getOrDefault(String key, boolean defaultValue) {
        if (contains(key)) {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            return (boolean) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the defined keys.
     *
     * @return a set containing all the defined keys
     */
    protected Set<String> keySet() {
        return values.keySet();
    }

    /**
     * Checks whether a value is stored for the given key.
     *
     * @param key
     *            the key to check
     * @return <code>true</code> if there is a value stored; <code>false</code>
     *         if no value is stored
     */
    protected boolean contains(String key) {
        setAccessed(key);
        return values.containsKey(key);
    }

    /**
     * Removes the value stored for the given key.
     *
     * @param key
     *            the key for which to remove the value
     */
    protected void remove(String key) {
        setChanged(key);
        Object oldValue = values.remove(key);
        detatchPotentialChild(oldValue);
    }

    /**
     * Removes the values for all stored keys.
     */
    protected void clear() {
        for (String key : new ArrayList<>(keySet())) {
            remove(key);
        }
    }

    private void setUnChanged(String key) {
        assert key != null;
        getChangeTracker().remove(key);
    }

    private void setChanged(String key) {
        assert key != null;

        getNode().markAsDirty();

        Map<String, Serializable> changes = getChangeTracker();

        if (!changes.containsKey(key)) {
            // Record this as changed for the collection logic
            if (values.containsKey(key)) {
                Serializable oldValue = values.get(key);
                changes.put(key, oldValue);
            } else {
                changes.put(key, REMOVED_MARKER);
            }
        }

        // TODO notify listeners
    }

    @Override
    protected HashMap<String, Serializable> createChangeTracker() {
        return new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected HashMap<String, Serializable> getChangeTracker() {
        // NodeFeature should really be generic on this type, but that would
        // cause so much ugliness in other parts of the code
        return (HashMap<String, Serializable>) super.getChangeTracker();
    }

    private void setAccessed(String key) {
        assert key != null;

        // TODO register listener
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        getChangeTracker().forEach((key, earlierValue) -> {
            boolean containsNow = values.containsKey(key);
            boolean containedEarlier = earlierValue != REMOVED_MARKER;
            if (containedEarlier && !containsNow) {
                collector.accept(new MapRemoveChange(this, key));
            } else if (containsNow) {
                Object currentValue = values.get(key);
                if (!containedEarlier
                        || !Objects.equals(earlierValue, currentValue)) {
                    // New or changed value
                    collector.accept(new MapPutChange(this, key, currentValue));
                }
            }
        });
    }

    @Override
    public void generateChangesFromEmpty() {
        Map<String, Serializable> changes = getChangeTracker();
        values.keySet().forEach(k -> changes.put(k, REMOVED_MARKER));
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        values.values().stream().filter(v -> v instanceof StateNode)
                .forEach(v -> action.accept((StateNode) v));
    }
}
