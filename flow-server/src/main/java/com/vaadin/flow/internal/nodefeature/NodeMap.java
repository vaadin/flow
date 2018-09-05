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

package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.MapRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.shared.util.UniqueSerializable;

/**
 * A state node feature that structures data as a map.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class NodeMap extends NodeFeature {
    private static final Serializable REMOVED_MARKER = new UniqueSerializable() {
    };

    private interface Values extends Serializable {
        int size();

        Serializable get(String key);

        Set<String> keySet();

        boolean containsKey(String key);

        default boolean isEmpty() {
            return size() == 0;
        }

        Stream<Serializable> streamValues();

        // Named set instead of put to avoid incompatibility with HashMap where
        // put returns the previous value
        void set(String key, Serializable value);
    }

    private static class SingleValue implements Values {

        private final String key;

        private Serializable value;

        public SingleValue(String key, Serializable value) {
            assert key != null;
            this.key = key;
            this.value = value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Serializable get(String key) {
            if (containsKey(key)) {
                return value;
            } else {
                return null;
            }
        }

        @Override
        public Set<String> keySet() {
            return Collections.singleton(key);
        }

        @Override
        public boolean containsKey(String key) {
            return this.key.equals(key);
        }

        @Override
        public Stream<Serializable> streamValues() {
            return Stream.of(value);
        }

        @Override
        public void set(String key, Serializable value) {
            assert key.equals(this.key);
            this.value = value;
        }
    }

    private static class HashMapValues extends HashMap<String, Serializable>
            implements Values {

        public HashMapValues(Values previousValues) {
            super(previousValues == null ? 0 : previousValues.size());
            if (previousValues != null) {
                previousValues.keySet().forEach(
                        key -> super.put(key, previousValues.get(key)));
            }
        }

        @Override
        public Serializable get(String key) {
            return super.get(key);
        }

        @Override
        public void set(String key, Serializable value) {
            super.put(key, value);
        }

        @Override
        public boolean containsKey(String key) {
            return super.containsKey(key);
        }

        @Override
        public Stream<Serializable> streamValues() {
            return super.values().stream();
        }
    }

    private Values values;

    private boolean isPopulated;

    /**
     * Creates a new map feature for the given node.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public NodeMap(StateNode node) {
        super(node);
        isPopulated = !node.isReportedFeature(getClass());
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
        put(key, value, true);
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
     * @return the previous value, or <code>null</code> if there was no previous
     *         value
     */
    protected Serializable put(String key, Serializable value,
            boolean emitChange) {
        Serializable oldValue = get(key);
        if (contains(key) && Objects.equals(oldValue, value)) {
            return oldValue;
        }
        if (emitChange) {
            setChanged(key);
        } else {
            setUnChanged(key);
        }

        // Optimize memory use when there's only one key
        if (values == null) {
            values = new SingleValue(key, value);
        } else {
            if (values instanceof SingleValue && !values.containsKey(key)) {
                values = new HashMapValues(values);
            }
            values.set(key, value);
        }

        detatchPotentialChild(oldValue);

        attachPotentialChild(value);

        return oldValue;
    }

    /**
     * Gets the value corresponding to the given key.
     *
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the key; <code>null</code> if there is
     *         no value stored, or if <code>null</code> is stored as a value
     */
    protected Serializable get(String key) {
        setAccessed(key);
        if (values == null) {
            return null;
        }
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
        if (values == null) {
            return Collections.emptySet();
        }
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
        if (values == null) {
            return false;
        }
        return values.containsKey(key);
    }

    /**
     * Removes the value stored for the given key.
     *
     * @param key
     *            the key for which to remove the value
     * @return the removed value, <code>null</code> if no value was removed
     */
    protected Serializable remove(String key) {
        setChanged(key);
        Serializable oldValue;

        if (values == null) {
            return null;
        } else if (values instanceof SingleValue) {
            oldValue = values.get(key);
            if (values.containsKey(key)) {
                values = null;
            }
        } else {
            assert values instanceof HashMapValues;
            HashMapValues hashMapValues = (HashMapValues) values;
            oldValue = hashMapValues.remove(key);

            if (hashMapValues.isEmpty()) {
                values = null;
            }
        }

        detatchPotentialChild(oldValue);

        return oldValue;
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
            if (values != null && values.containsKey(key)) {
                Serializable oldValue = values.get(key);
                changes.put(key, oldValue);
            } else {
                changes.put(key, REMOVED_MARKER);
            }
        }

        // TODO notify listeners
    }

    private Map<String, Serializable> getChangeTracker() {
        return getNode().getChangeTracker(this, HashMap::new);
    }

    private void setAccessed(String key) {
        assert key != null;

        // TODO register listener
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        boolean hasChanges = false;
        for (Entry<String, Serializable> entry : getChangeTracker()
                .entrySet()) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            boolean containsNow = values != null && values.containsKey(key);
            boolean containedEarlier = value != REMOVED_MARKER;
            if (containedEarlier && !containsNow) {
                collector.accept(new MapRemoveChange(this, key));
                hasChanges = true;
            } else if (containsNow) {
                Object currentValue = values.get(key);
                if (!containedEarlier || !Objects.equals(value, currentValue)) {
                    // New or changed value
                    collector.accept(new MapPutChange(this, key, currentValue));
                    hasChanges = true;
                }
            }
        }
        if (!isPopulated) {
            if (!hasChanges) {
                collector.accept(new EmptyChange(this));
            }
            isPopulated = true;
        }
    }

    @Override
    public void generateChangesFromEmpty() {
        if (values == null) {
            if (!isPopulated) {
                // populate change tracker so that an empty change can be
                // reported
                getChangeTracker();
            }
            return;
        }
        assert !values.isEmpty();

        Map<String, Serializable> changes = getChangeTracker();
        values.keySet().forEach(k -> changes.put(k, REMOVED_MARKER));
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        if (values == null) {
            return;
        }
        assert !values.isEmpty();

        values.streamValues().filter(v -> v instanceof StateNode)
                .forEach(v -> action.accept((StateNode) v));
    }

    /**
     * Receives a value update from the client. The map value is updated without
     * creating a change record since the client already knows the current
     * value. The value is only updated if
     * {@link #mayUpdateFromClient(String, Serializable)} has been overridden to
     * accept the value.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to store
     */
    public void updateFromClient(String key, Serializable value) {
        if (!mayUpdateFromClient(key, value)) {
            throw new IllegalArgumentException(String.format(
                    "Feature '%s' doesn't allow the client to update '%s'",
                    getClass().getName(), key));
        }

        put(key, value, false);
    }

    /**
     * Checks whether the client is allowed to store the given value with the
     * given key. Always returns <code>false</code> by default.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to store
     * @return <code>true</code> if the value update is accepted,
     *         <code>false</code> if the value should not be allowed to be
     *         updated
     */
    protected boolean mayUpdateFromClient(String key, Serializable value) {
        return false;
    }

    // Exposed for testing purposes
    boolean usesSingleMap() {
        return values instanceof SingleValue;
    }

}
