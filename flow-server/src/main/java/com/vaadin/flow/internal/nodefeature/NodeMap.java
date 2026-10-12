/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.dom.SignalBinding;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.MapRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.shared.util.UniqueSerializable;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * A state node feature that structures data as a map.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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

        /**
         * Stores a value for the given key.
         *
         * @param key
         *            the key to use
         * @param value
         *            the value to store
         * @return the instance to use from now on, either this instance or a
         *         new instance if the set of keys changed
         */
        Values put(String key, Serializable value);

        /**
         * Removes the value for the given key, which must be present.
         *
         * @param key
         *            the key to remove
         * @return the instance to use from now on, or <code>null</code> if no
         *         values remain
         */
        @Nullable
        Values remove(String key);
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
        public Values put(String key, Serializable value) {
            if (this.key.equals(key)) {
                this.value = value;
                return this;
            }
            SharedKeySet keySet = SharedKeySet
                    .of(new String[] { this.key, key });
            SharedKeySetValues result = SharedKeySetValues.create(keySet);
            result.setValue(keySet.indexOf(this.key), this.value);
            result.setValue(keySet.indexOf(key), value);
            return result;
        }

        @Override
        public @Nullable Values remove(String key) {
            assert containsKey(key);
            return null;
        }
    }

    /**
     * An immutable set of keys that is shared between all
     * {@link SharedKeySetValues} instances that contain exactly those keys,
     * regardless of the order in which the keys were added. The key order of
     * the instance defines the order of the values in
     * {@link SharedKeySetValues}. Instances are canonicalized through a global
     * registry so that each distinct set of keys is stored only once regardless
     * of how many maps use it. The registry only holds weak references so that
     * key sets that are no longer used by any map can be garbage collected.
     */
    private static final class SharedKeySet extends AbstractSet<String>
            implements Serializable {

        /**
         * Key sets larger than this use a map for finding the index of a key
         * instead of a linear scan.
         */
        private static final int INDEX_MAP_THRESHOLD = 8;

        private static final class RegistryReference
                extends WeakReference<SharedKeySet> {
            private final Set<String> registryKey;

            private RegistryReference(SharedKeySet keySet,
                    Set<String> registryKey) {
                super(keySet, staleReferences);
                this.registryKey = registryKey;
            }
        }

        private static final ConcurrentHashMap<Set<String>, RegistryReference> registry = new ConcurrentHashMap<>();

        private static final ReferenceQueue<SharedKeySet> staleReferences = new ReferenceQueue<>();

        private final String[] keys;

        private final transient @Nullable Map<String, Integer> indices;

        private SharedKeySet(String[] keys) {
            this.keys = keys;
            if (keys.length > INDEX_MAP_THRESHOLD) {
                indices = new HashMap<>(keys.length * 2);
                for (int i = 0; i < keys.length; i++) {
                    indices.put(keys[i], i);
                }
            } else {
                indices = null;
            }
        }

        /**
         * Gets the canonical key set instance for the given keys. The key order
         * of the returned instance may differ from the order of the given keys
         * if a key set with the same keys is already registered.
         *
         * @param keys
         *            the keys without duplicates. The array is retained and
         *            must not be modified afterwards.
         * @return the canonical key set, not <code>null</code>
         */
        private static SharedKeySet of(String[] keys) {
            removeStaleReferences();

            // Must not reference the SharedKeySet instance since it's used as
            // a key in the registry that only weakly references the instance
            Set<String> registryKey = Set.of(keys);
            RegistryReference reference = registry.get(registryKey);
            SharedKeySet keySet = reference == null ? null : reference.get();
            if (keySet != null) {
                return keySet;
            }

            SharedKeySet[] result = new SharedKeySet[1];
            registry.compute(registryKey, (key, existing) -> {
                SharedKeySet existingKeySet = existing == null ? null
                        : existing.get();
                if (existingKeySet != null) {
                    result[0] = existingKeySet;
                    return existing;
                }
                result[0] = new SharedKeySet(keys);
                return new RegistryReference(result[0], key);
            });
            return result[0];
        }

        private static void removeStaleReferences() {
            Reference<? extends SharedKeySet> stale;
            while ((stale = staleReferences.poll()) != null) {
                RegistryReference reference = (RegistryReference) stale;
                registry.remove(reference.registryKey, reference);
            }
        }

        private int indexOf(String key) {
            if (indices != null) {
                Integer index = indices.get(key);
                return index == null ? -1 : index;
            }
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(key)) {
                    return i;
                }
            }
            return -1;
        }

        private SharedKeySet withKey(String key) {
            String[] newKeys = Arrays.copyOf(keys, keys.length + 1);
            newKeys[keys.length] = key;
            return of(newKeys);
        }

        private SharedKeySet withoutKey(int index) {
            return of(removeIndex(keys, new String[keys.length - 1], index));
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof String key && indexOf(key) >= 0;
        }

        @Override
        public Iterator<String> iterator() {
            return Arrays.asList(keys).iterator();
        }

        @Override
        public int size() {
            return keys.length;
        }
    }

    /**
     * Base class for values with multiple keys. Stores the values in the order
     * defined by a {@link SharedKeySet}, with specialized subclasses for small
     * sizes to avoid the overhead of a separate array.
     */
    private abstract static class SharedKeySetValues implements Values {

        private final SharedKeySet keySet;

        private SharedKeySetValues(SharedKeySet keySet) {
            this.keySet = keySet;
        }

        private static SharedKeySetValues create(SharedKeySet keySet) {
            return switch (keySet.size()) {
            case 2 -> new TwoValues(keySet);
            case 3 -> new ThreeValues(keySet);
            case 4 -> new FourValues(keySet);
            case 5 -> new FiveValues(keySet);
            default -> new ArrayValues(keySet);
            };
        }

        abstract Serializable getValue(int index);

        abstract void setValue(int index, Serializable value);

        @Override
        public int size() {
            return keySet.size();
        }

        @Override
        public Serializable get(String key) {
            int index = keySet.indexOf(key);
            return index < 0 ? null : getValue(index);
        }

        @Override
        public Set<String> keySet() {
            return keySet;
        }

        @Override
        public boolean containsKey(String key) {
            return keySet.indexOf(key) >= 0;
        }

        @Override
        public Stream<Serializable> streamValues() {
            return IntStream.range(0, size()).mapToObj(this::getValue);
        }

        @Override
        public Values put(String key, Serializable value) {
            int index = keySet.indexOf(key);
            if (index >= 0) {
                setValue(index, value);
                return this;
            }

            SharedKeySetValues result = copyTo(keySet.withKey(key));
            result.setValue(result.keySet.indexOf(key), value);
            return result;
        }

        @Override
        public Values remove(String key) {
            int index = keySet.indexOf(key);
            assert index >= 0;

            if (size() == 2) {
                int remaining = 1 - index;
                return new SingleValue(keySet.keys[remaining],
                        getValue(remaining));
            }
            return copyTo(keySet.withoutKey(index));
        }

        /**
         * Creates a new instance for the given key set with the current values
         * copied into the order defined by that key set. Keys that are not
         * present in the current key set get a <code>null</code> value.
         */
        private SharedKeySetValues copyTo(SharedKeySet target) {
            SharedKeySetValues result = create(target);
            for (int i = 0; i < target.size(); i++) {
                int sourceIndex = keySet.indexOf(target.keys[i]);
                if (sourceIndex >= 0) {
                    result.setValue(i, getValue(sourceIndex));
                }
            }
            return result;
        }

        protected Object readResolve() {
            // The registry may define a different key order for the same keys
            // than the one that was serialized
            return copyTo(SharedKeySet.of(keySet.keys));
        }
    }

    private static class TwoValues extends SharedKeySetValues {
        private Serializable value0;
        private Serializable value1;

        private TwoValues(SharedKeySet keySet) {
            super(keySet);
        }

        @Override
        Serializable getValue(int index) {
            return switch (index) {
            case 0 -> value0;
            case 1 -> value1;
            default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        void setValue(int index, Serializable value) {
            switch (index) {
            case 0 -> value0 = value;
            case 1 -> value1 = value;
            default -> throw new IndexOutOfBoundsException(index);
            }
        }
    }

    private static class ThreeValues extends SharedKeySetValues {
        private Serializable value0;
        private Serializable value1;
        private Serializable value2;

        private ThreeValues(SharedKeySet keySet) {
            super(keySet);
        }

        @Override
        Serializable getValue(int index) {
            return switch (index) {
            case 0 -> value0;
            case 1 -> value1;
            case 2 -> value2;
            default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        void setValue(int index, Serializable value) {
            switch (index) {
            case 0 -> value0 = value;
            case 1 -> value1 = value;
            case 2 -> value2 = value;
            default -> throw new IndexOutOfBoundsException(index);
            }
        }
    }

    private static class FourValues extends SharedKeySetValues {
        private Serializable value0;
        private Serializable value1;
        private Serializable value2;
        private Serializable value3;

        private FourValues(SharedKeySet keySet) {
            super(keySet);
        }

        @Override
        Serializable getValue(int index) {
            return switch (index) {
            case 0 -> value0;
            case 1 -> value1;
            case 2 -> value2;
            case 3 -> value3;
            default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        void setValue(int index, Serializable value) {
            switch (index) {
            case 0 -> value0 = value;
            case 1 -> value1 = value;
            case 2 -> value2 = value;
            case 3 -> value3 = value;
            default -> throw new IndexOutOfBoundsException(index);
            }
        }
    }

    private static class FiveValues extends SharedKeySetValues {
        private Serializable value0;
        private Serializable value1;
        private Serializable value2;
        private Serializable value3;
        private Serializable value4;

        private FiveValues(SharedKeySet keySet) {
            super(keySet);
        }

        @Override
        Serializable getValue(int index) {
            return switch (index) {
            case 0 -> value0;
            case 1 -> value1;
            case 2 -> value2;
            case 3 -> value3;
            case 4 -> value4;
            default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        void setValue(int index, Serializable value) {
            switch (index) {
            case 0 -> value0 = value;
            case 1 -> value1 = value;
            case 2 -> value2 = value;
            case 3 -> value3 = value;
            case 4 -> value4 = value;
            default -> throw new IndexOutOfBoundsException(index);
            }
        }
    }

    /**
     * Stores values in an exactly sized array. Used for key sets that are too
     * large for the specialized classes with one field per value.
     */
    private static class ArrayValues extends SharedKeySetValues {
        private final Serializable[] values;

        private ArrayValues(SharedKeySet keySet) {
            super(keySet);
            values = new Serializable[keySet.size()];
        }

        @Override
        Serializable getValue(int index) {
            return values[index];
        }

        @Override
        void setValue(int index, Serializable value) {
            values[index] = value;
        }

        @Override
        public Stream<Serializable> streamValues() {
            return Arrays.stream(values);
        }
    }

    private static <T> T[] removeIndex(T[] source, T[] target, int index) {
        System.arraycopy(source, 0, target, 0, index);
        System.arraycopy(source, index + 1, target, index,
                source.length - index - 1);
        return target;
    }

    /**
     * @since 25.1
     */
    public record InternalSignalBinding(Signal<?> signal, Serializable value,
            SerializableConsumer<?> writeCallback) implements Serializable {
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
        return putInternal(key, value, emitChange);
    }

    private Serializable putInternal(String key, Serializable value,
            boolean emitChange) {
        Serializable oldValue = get(key);
        if (!producePutChange(key, contains(key), value)) {
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
            values = values.put(key, value);
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
        return doGet(key);
    }

    private Serializable doGet(String key) {
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

        if (values == null || !values.containsKey(key)) {
            return null;
        }

        Serializable oldValue = values.get(key);
        values = values.remove(key);

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
            } else if (containsNow
                    && producePutChange(key, containedEarlier, value)) {
                Object currentValue = values.get(key);
                if (currentValue instanceof InternalSignalBinding binding) {
                    currentValue = binding.value();
                }
                // New or changed value
                collector.accept(new MapPutChange(this, key, currentValue));
                hasChanges = true;
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

    /**
     * Checks whether a {@link MapPutChange} should be produced.
     *
     * @param key
     *            a key to produce a change
     * @param hadValueEarlier
     *            whether the value was already earlier in the map
     * @param newValue
     *            the new value for the {@code key}
     * @return {@code true} if a change should be produced, {@code false}
     *         otherwise
     * @since 4.0
     */
    protected boolean producePutChange(String key, boolean hadValueEarlier,
            Serializable newValue) {
        return !hadValueEarlier || !Objects.equals(newValue, values.get(key));
    }

    // Exposed for testing purposes
    boolean usesSingleMap() {
        return values instanceof SingleValue;
    }

    /**
     * Binds a signal to a given key using a setter function and stores the
     * writeCallback in SignalBindingFeature.
     *
     * @param owner
     *            the element owning the key, not <code>null</code>
     * @param key
     *            the key of the node map
     * @param signal
     *            the signal to bind, not <code>null</code>
     * @param setter
     *            the function to call when the signal value changes
     * @param writeCallback
     *            the callback to propagate value changes back, or
     *            <code>null</code> for a read-only binding
     * @param <T>
     *            the type of the signal value
     *
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding for the
     *             given key
     *
     * @since 25.1
     */
    protected <T extends @Nullable Object> SignalBinding<T> bindSignal(
            Element owner, String key, Signal<T> signal,
            SerializableBiConsumer<Element, T> setter,
            SerializableConsumer<?> writeCallback) {
        Objects.requireNonNull(signal, "Signal cannot be null");
        InternalSignalBinding previousBinding;
        if (doGet(key) instanceof InternalSignalBinding binding) {
            previousBinding = binding;
        } else {
            previousBinding = null;
        }
        if (previousBinding != null && previousBinding.signal() != null) {
            throw new BindingActiveException();
        }

        // Setter might trigger an immediate value update which requires the
        // signal to be registered before ElementEffect.bind call.
        putInternal(key,
                new InternalSignalBinding(signal, get(key), writeCallback),
                false);

        return ElementEffect.bind(owner, signal, setter);
    }

    /**
     * Checks whether there is a signal binding for the given key.
     * 
     * @param key
     *            the key to check
     * @return true if there is an active signal binding, false otherwise
     * @since 25.1
     */
    public boolean hasSignal(String key) {
        return doGet(key) instanceof InternalSignalBinding binding
                && binding.signal() != null;
    }
}
