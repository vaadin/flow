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
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.Range;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * HierarchicalCommunicationController controls all the communication to client.
 * 
 * @param <T>
 *            the target bean type
 * @since 1.2
 */
public class HierarchicalCommunicationController<T> implements Serializable {

    private final DataKeyMapper<T> keyMapper;
    private final DataGenerator<T> dataGenerator;
    private final SerializableFunction<Integer, HierarchicalUpdate> startUpdate;
    private final HierarchyMapper<T, ?> mapper;
    private final SerializableBiFunction<String, Range, Stream<T>> fetchItems;

    // position in tree this object controls child items for. null means root.
    private final String parentKey;

    // Last total size value sent to the client
    private int assumedSize;

    // The range of items that the client wants to have
    private Range requestedRange = Range.between(0, 0);

    // Items that have been synced to the client and not yet passivated
    private int activeStart = 0;

    // ArrayList or emptyList(), both are serializable
    private List<String> activeKeyOrder = Collections.emptyList();

    private boolean resendEntireRange = true;
    private boolean assumeEmptyClient = true;

    private int nextUpdateId = 0;

    // Keys that can be discarded once some specific update id gets confirmed
    private final HashMap<Integer, Set<String>> passivatedByUpdate = new HashMap<>();

    // Update ids that have been confirmed since the last flush
    private final HashSet<Integer> confirmedUpdates = new HashSet<>();

    /**
     * Constructs communication controller with support for hierarchical data
     * structure.
     * 
     * @param parentKey
     *            parent key or null if root
     * @param keyMapper
     *            Object to String key mapper
     * @param mapper
     *            Mapper for hierarchical data
     * @param dataGenerator
     *            A data generator for items
     * @param startUpdate
     *            Function for creating a new {@link Update} for client
     * @param fetchItems
     *            Function for fetching items for target parent and specified
     *            range
     */
    public HierarchicalCommunicationController(String parentKey,
            DataKeyMapper<T> keyMapper, HierarchyMapper<T, ?> mapper,
            DataGenerator<T> dataGenerator,
            SerializableFunction<Integer, HierarchicalUpdate> startUpdate,
            SerializableBiFunction<String, Range, Stream<T>> fetchItems) {
        this.parentKey = parentKey;
        this.keyMapper = keyMapper;
        this.mapper = mapper;
        this.dataGenerator = dataGenerator;
        this.startUpdate = startUpdate;
        this.fetchItems = fetchItems;
    }

    public void flush() {
        Set<String> oldActive = new HashSet<>(activeKeyOrder);

        assumedSize = mapper.countChildItems(keyMapper.get(parentKey));

        final Range previousActive = Range.withLength(activeStart,
                activeKeyOrder.size());
        final Range effectiveRequested = requestedRange
                .restrictTo(Range.withLength(0, assumedSize));

        resendEntireRange |= !(previousActive.intersects(effectiveRequested)
                || (previousActive.isEmpty() && effectiveRequested.isEmpty()));

        // Phase 1: Find all items that the client should have
        List<String> newActiveKeyOrder = collectKeysToFlush(previousActive,
                effectiveRequested);

        activeKeyOrder = newActiveKeyOrder;
        activeStart = effectiveRequested.getStart();

        // Phase 2: Collect changes to send
        HierarchicalUpdate update = startUpdate.apply(assumedSize);
        boolean updated = collectChangesToSend(previousActive,
                effectiveRequested, update);

        resendEntireRange = false;
        assumeEmptyClient = false;

        // Phase 3: passivate anything that isn't longer active
        passivateInactiveKeys(oldActive, newActiveKeyOrder, update, updated);

        // Phase 4: unregister passivated and updated items
        unregisterPassivatedKeys();
    }

    public void confirmUpdate(int updateId) {
        confirmedUpdates.add(Integer.valueOf(updateId));
    }

    public void setRequestRange(int start, int length) {
        requestedRange = Range.withLength(start, length);
    }

    public void setResendEntireRange(boolean resend) {
        resendEntireRange = resend;
    }

    private boolean collectChangesToSend(final Range previousActive,
            final Range effectiveRequested, HierarchicalUpdate update) {
        boolean updated = false;
        if (assumeEmptyClient || resendEntireRange) {
            if (!assumeEmptyClient) {
                /*
                 * TODO: Not necessary to clear something that would be set back
                 * a few lines later in the code.
                 *
                 * It's not that straightforward because one has to care about
                 * indexes aligned with pageSize (because of the code on the
                 * client side).
                 */
                clear(previousActive.getStart(), previousActive.length(),
                        update);
            }

            set(effectiveRequested, update);
            updated = true;
        } else if (!previousActive.equals(effectiveRequested)) {
            /*
             * There are some parts common between what we have and what we
             * should have, but the beginning and/or the end has too many or too
             * few items.
             */

            // Clear previously active items missing from requested
            withMissing(previousActive, effectiveRequested,
                    range -> clear(range.getStart(), range.length(), update));

            // Set requested items missing from previously active
            withMissing(effectiveRequested, previousActive,
                    range -> set(range, update));
            updated = true;
        }
        return updated;
    }

    private void set(Range effectiveRequested, HierarchicalUpdate update) {
        if (effectiveRequested.isEmpty() || activeKeyOrder.isEmpty()
                || effectiveRequested.getStart() >= assumedSize) {
            return;
        }
        if (parentKey == null) {
            update.set(effectiveRequested.getStart(),
                    getJsonItems(effectiveRequested));
        } else {
            update.set(effectiveRequested.getStart(),
                    getJsonItems(effectiveRequested), parentKey);
        }
    }

    private void clear(int start, int length, HierarchicalUpdate update) {
        if (length == 0 || start >= assumedSize) {
            return;
        }
        if (parentKey == null) {
            update.clear(start, length);
        } else {
            update.clear(start, length, parentKey);
        }
    }

    private List<String> collectKeysToFlush(final Range previousActive,
            final Range effectiveRequested) {
        List<String> newActiveKeyOrder;
        /*
         * Collecting all items even though only some small sub range would
         * actually be useful can be optimized away once we have some actual
         * test coverage for the logic here.
         */
        if (resendEntireRange) {
            newActiveKeyOrder = activate(effectiveRequested);
        } else {
            Range[] partitionWith = effectiveRequested
                    .partitionWith(previousActive);

            newActiveKeyOrder = new ArrayList<>();
            newActiveKeyOrder.addAll(activate(partitionWith[0]));

            // Pick existing items from the current list
            Range overlap = partitionWith[1].offsetBy(-activeStart);
            newActiveKeyOrder.addAll(activeKeyOrder.subList(overlap.getStart(),
                    overlap.getEnd()));

            newActiveKeyOrder.addAll(activate(partitionWith[2]));
        }
        return newActiveKeyOrder;
    }

    private List<String> activate(Range range) {
        if (range.isEmpty()) {
            return Collections.emptyList();
        }

        // XXX Explicitly refresh anything that is updated
        List<String> activeKeys = new ArrayList<>(range.length());

        fetchItems.apply(parentKey, range).forEach(bean -> {
            boolean mapperHasKey = keyMapper.has(bean);
            String key = keyMapper.key(bean);
            if (mapperHasKey) {
                passivatedByUpdate.values()
                        .forEach(set -> set.remove(key));
            }
            activeKeys.add(key);
        });
        return activeKeys;
    }

    private void passivateInactiveKeys(Set<String> oldActive,
            List<String> newActiveKeyOrder, HierarchicalUpdate update,
            boolean updated) {
        /*
         * We cannot immediately unregister keys that we have asked the client
         * to remove, since the client might send a message using that key
         * before our message about removal arrives at the client and is
         * applied.
         */
        if (updated) {
            int updateId = nextUpdateId++;

            if (parentKey == null) {
                update.commit(updateId);
            } else {
                update.commit(updateId, parentKey, assumedSize);
            }

            // Finally clear any passivated items that have now been confirmed
            oldActive.removeAll(newActiveKeyOrder);
            if (!oldActive.isEmpty()) {
                passivatedByUpdate.put(Integer.valueOf(updateId), oldActive);
            }
        }
    }

    public void unregisterPassivatedKeys() {
        /*
         * Actually unregister anything that was removed in an update that the
         * client has confirmed that it has applied.
         */
        if (!confirmedUpdates.isEmpty()) {
            confirmedUpdates.forEach(this::doUnregister);
            confirmedUpdates.clear();
        }
    }

    private void doUnregister(Integer updateId) {
        Set<String> passivated = passivatedByUpdate.remove(updateId);
        if (passivated != null) {
            passivated.forEach(key -> {
                T item = keyMapper.get(key);
                if (item != null) {
                    dataGenerator.destroyData(item);
                    keyMapper.remove(item);
                }
            });
        }
    }

    private List<JsonValue> getJsonItems(Range range) {
        return range.stream()
                .mapToObj(index -> activeKeyOrder.get(index - activeStart))
                .map(keyMapper::get).map(this::generateJson)
                .collect(Collectors.toList());
    }

    public JsonValue generateJson(T item) {
        JsonObject json = Json.createObject();
        json.put("key", keyMapper.key(item));
        dataGenerator.generateData(item, json);
        return json;
    }

    private static final void withMissing(Range expected, Range actual,
            Consumer<Range> action) {
        Range[] partition = expected.partitionWith(actual);

        applyIfNotEmpty(partition[0], action);
        applyIfNotEmpty(partition[2], action);
    }

    private static final void applyIfNotEmpty(Range range,
            Consumer<Range> action) {
        if (!range.isEmpty()) {
            action.accept(range);
        }
    }

}
