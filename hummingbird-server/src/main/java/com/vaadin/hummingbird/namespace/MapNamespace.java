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

package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.MapRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;

/**
 * A state node namespace that structures data as a map.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class MapNamespace extends Namespace {
    private static final Object REMOVED_MARKER = new Object();

    private Map<String, Object> values = new HashMap<>();

    private Map<String, Object> changes = new HashMap<>();

    /**
     * Creates a new map namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public MapNamespace(StateNode node) {
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
    protected void put(String key, Object value) {
        attachPotentialChild(value);

        setChanged(key);
        Object oldValue = values.put(key, value);

        detatchPotentialChild(oldValue);
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

    private void setChanged(String key) {
        assert key != null;

        getNode().markAsDirty();

        if (!changes.containsKey(key)) {
            // Record this as changed for the collection logic
            if (values.containsKey(key)) {
                Object oldValue = values.get(key);
                changes.put(key, oldValue);
            } else {
                changes.put(key, REMOVED_MARKER);
            }
        }

        // TODO notify listeners
    }

    private void setAccessed(String key) {
        assert key != null;

        // TODO register listener
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        changes.forEach((key, earlierValue) -> {
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
        changes.clear();
    }

    @Override
    public void resetChanges() {
        changes.clear();
        values.keySet().forEach(k -> changes.put(k, REMOVED_MARKER));
    }

}
