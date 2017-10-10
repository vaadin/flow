/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.StateNode;
import com.vaadin.function.SerializableConsumer;
import com.vaadin.util.Range;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * DataProvider base class. This class is the base for all DataProvider
 * communication implementations. It uses data generators ({@link BiFunction}s)
 * to write {@link JsonObject}s representing each data object to be sent to the
 * client-side.
 *
 * @param <T>
 *            the bean type
 *
 */
public class DataCommunicator<T> {
    private final BiFunction<String, T, JsonValue> dataGenerator;
    private final ArrayUpdater arrayUpdater;
    private final StateNode stateNode;

    private final KeyMapper<T> keyMapper = new KeyMapper<>();

    // The range of items that the client wants to have
    private Range requestedRange = Range.between(0, 0);

    // Items that have been synced to the client and not yet passivated
    private int activeStart = 0;
    private List<String> activeKeyOrder = Collections.emptyList();

    // Last total size value sent to the client
    private int assumedSize;

    private boolean resendEntireRange = true;
    private boolean assumeEmptyClient = true;

    private int nextUpdateId = 0;

    // Keys that can be discarded once some specific update id gets confirmed
    private final Map<Integer, Set<String>> passivatedByUpdate = new HashMap<>();

    // Update ids that have been confirmed since the last flush
    private final Set<Integer> confirmedUpdates = new HashSet<>();

    private DataProvider<T, ?> dataProvider = DataProvider.ofItems();
    private Object filter;
    private Comparator<T> inMemorySorting;
    private final List<QuerySortOrder> backEndSorting = new ArrayList<>();

    private Runnable flushRequest;

    /**
     * Creates a new instance.
     *
     * @param dataGenerator
     *            the data generator function
     * @param arrayUpdater
     *            array updater strategy
     * @param stateNode
     *            the state node used to communicate for
     */
    public DataCommunicator(BiFunction<String, T, JsonValue> dataGenerator,
            ArrayUpdater arrayUpdater, StateNode stateNode) {
        this.dataGenerator = dataGenerator;
        this.arrayUpdater = arrayUpdater;
        this.stateNode = stateNode;

        stateNode.addAttachListener(this::handleAttach);
        stateNode.addDetachListener(this::handleDetach);

        requestFlush();
    }

    /**
     * Sets the requested range of data to be sent.
     *
     * @param start
     *            the start of the requested range
     * @param length
     *            the end of the requested range
     */
    public void setRequestedRange(int start, int length) {
        requestedRange = Range.withLength(start, length);

        requestFlush();
    }

    /**
     * Resets all the data.
     * <p>
     * It effectively resends all available data.
     */
    public void reset() {
        resendEntireRange = true;

        requestFlush();
    }

    /**
     * Confirm update with the given {@code updateId}.
     *
     * @param updateId
     *            the update identifier
     */
    public void confirmUpdate(int updateId) {
        confirmedUpdates.add(Integer.valueOf(updateId));

        // Not absolutely necessary, but doing it right away to release memory
        // earlier
        requestFlush();
    }

    /**
     * Gets the current data provider from this DataCommunicator.
     *
     * @return the data provider
     */
    public DataProvider<T, ?> getDataProvider() {
        return dataProvider;
    }

    /**
     * Sets the current data provider for this DataCommunicator.
     * <p>
     * The returned consumer can be used to set some other filter value that
     * should be included in queries sent to the data provider. It is only valid
     * until another data provider is set.
     *
     * @param dataProvider
     *            the data provider to set, not <code>null</code>
     * @param initialFilter
     *            the initial filter value to use, or <code>null</code> to not
     *            use any initial filter value
     *
     * @param <F>
     *            the filter type
     *
     * @return a consumer that accepts a new filter value to use
     */
    public <F> SerializableConsumer<F> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter) {
        Objects.requireNonNull(dataProvider, "data provider cannot be null");
        filter = initialFilter;

        // TODO: remove old data provider listener (see
        // handleAttach/handleDetach)

        reset();

        this.dataProvider = dataProvider;

        keyMapper.setIdentifierGetter(dataProvider::getId);

        // TODO: add data provider listener if attached (see handleAttach)

        return filter -> {
            if (this.dataProvider != dataProvider) {
                throw new IllegalStateException(
                        "Filter slot is no longer valid after data provider has been changed");
            }

            if (!Objects.equals(this.filter, filter)) {
                this.filter = filter;
                reset();
            }
        };
    }

    /**
     * Gets the {@link DataKeyMapper} used by this {@link DataCommunicator}. Key
     * mapper can be used to map keys sent to the client-side back to their
     * respective data objects.
     *
     * @return key mapper
     */
    public DataKeyMapper<T> getKeyMapper() {
        return keyMapper;
    }

    /**
     * Getter method for finding the size of DataProvider. Can be overridden by
     * a subclass that uses a specific type of DataProvider and/or query.
     *
     * @return the size of data provider with current filter
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected int getDataProviderSize() {
        return getDataProvider().size(new Query(getFilter()));
    }

    /**
     * Get the object used for filtering in this data communicator.
     *
     * @return the filter object of this data communicator
     */
    protected Object getFilter() {
        return filter;
    }

    /**
     * Fetches a list of items from the DataProvider.
     *
     * @param offset
     *            the starting index of the range
     * @param limit
     *            the max number of results
     * @return the list of items in given range
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Stream<T> fetchFromProvider(int offset, int limit) {
        return getDataProvider().fetch(new Query(offset, limit, backEndSorting,
                inMemorySorting, filter));
    }

    private void handleAttach() {
        // TODO: Add data listener
        /*
         * It should add data provider listener to listen events like
         * "refresh an item" event. As a result the item should be refreshed
         * here in the communicator class and data generators should refresh the
         * item. At the moment there is no dedicated DataGenerator interface so
         * it's not implemented. See
         * https://github.com/vaadin/framework/blob/master/server/src/main/java/
         * com/vaadin/data/provider/DataCommunicator.java in the FW8 for
         * details.
         */
    }

    private void handleDetach() {
        // TODO: Remove data listener
        /*
         * Just remove listener added in the handleAttach method via
         * registration
         */
    }

    private void requestFlush() {
        if (flushRequest == null) {
            flushRequest = () -> {
                flush();
                flushRequest = null;
            };
            stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                    .beforeClientResponse(stateNode, flushRequest));
        }
    }

    private void flush() {
        Set<String> oldActive = new HashSet<>(activeKeyOrder);

        if (resendEntireRange) {
            assumedSize = getDataProviderSize();
        }

        final Range previousActive = Range.withLength(activeStart,
                activeKeyOrder.size());
        final Range effectiveRequested = requestedRange
                .restrictTo(Range.withLength(0, assumedSize));

        resendEntireRange |= !previousActive.intersects(effectiveRequested);

        // Phase 1: Find all items that the client should have
        List<String> newActiveKeyOrder = collectKeysToFlush(previousActive,
                effectiveRequested);

        activeKeyOrder = newActiveKeyOrder;
        activeStart = effectiveRequested.getStart();

        // Phase 2: Collect changes to send
        Update update = arrayUpdater.startUpdate(assumedSize);
        boolean updated = collectChangesToSend(previousActive,
                effectiveRequested, update);

        resendEntireRange = false;
        assumeEmptyClient = false;

        // Phase 3: passivate anything that isn't longer active
        passivateInactiveKeys(oldActive, newActiveKeyOrder, update, updated);

        // Phase 4: unregister passivated and updated items
        unregisterPassivatedKeys();
    }

    private void unregisterPassivatedKeys() {
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
            passivated.forEach(key -> keyMapper.remove(keyMapper.get(key)));
        }
    }

    private void passivateInactiveKeys(Set<String> oldActive,
            List<String> newActiveKeyOrder, Update update, boolean updated) {
        /*
         * We cannot immediately unregister keys that we have asked the client
         * to remove, since the client might send a message using that key
         * before our message about removal arrives at the client and is
         * applied.
         */
        if (updated) {
            int updateId = nextUpdateId++;
            update.commit(updateId);

            // Finally clear any passivated items that have now been confirmed
            oldActive.removeAll(newActiveKeyOrder);
            if (!oldActive.isEmpty()) {
                passivatedByUpdate.put(Integer.valueOf(updateId), oldActive);
            }
        }
    }

    private boolean collectChangesToSend(final Range previousActive,
            final Range effectiveRequested, Update update) {
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
                update.clear(previousActive.getStart(),
                        previousActive.length());
            }

            update.set(activeStart, getJsonItems(effectiveRequested));
            updated = true;
        } else if (!previousActive.equals(effectiveRequested)) {
            /*
             * There are some parts common between what we have and what we
             * should have, but the beginning and/or the end has too many or too
             * few items.
             */

            // Clear previously active items missing from requested
            withMissing(previousActive, effectiveRequested,
                    range -> update.clear(range.getStart(), range.length()));

            // Set requested items missing from previously active
            withMissing(effectiveRequested, previousActive,
                    range -> update.set(range.getStart(), getJsonItems(range)));
            updated = true;
        }
        return updated;
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

    private List<JsonValue> getJsonItems(Range range) {
        return range.stream()
                .mapToObj(index -> activeKeyOrder.get(index - activeStart))
                .map(keyMapper::get).map(this::generateJson)
                .collect(Collectors.toList());
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

    private List<String> activate(Range range) {
        if (range.isEmpty()) {
            return Collections.emptyList();
        }

        // XXX Explicitly refresh anything that is updated
        List<String> activeKeys = new ArrayList<>(range.length());
        fetchFromProvider(range.getStart(), range.length())
                .forEach(bean -> {
                    boolean mapperHasKey = keyMapper.has(bean);
                    String key = keyMapper.key(bean);
                    if (mapperHasKey) {
                        passivatedByUpdate.values().stream()
                                .forEach(set -> set.remove(key));
                    }
                    activeKeys.add(key);
                });
        return activeKeys;
    }

    private JsonValue generateJson(T item) {
        return dataGenerator.apply(keyMapper.key(item), item);
    }


}
