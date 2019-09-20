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
package com.vaadin.flow.data.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonArray;
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
 * @since 1.0
 */
public class DataCommunicator<T> implements Serializable {
    private final DataGenerator<T> dataGenerator;
    private final ArrayUpdater arrayUpdater;
    private final SerializableConsumer<JsonArray> dataUpdater;
    private final StateNode stateNode;

    private DataKeyMapper<T> keyMapper = new KeyMapper<>();

    // The range of items that the client wants to have
    private Range requestedRange = Range.between(0, 0);

    // Items that have been synced to the client and not yet passivated
    private int activeStart = 0;

    // ArrayList or emptyList(), both are serializable
    private List<String> activeKeyOrder = Collections.emptyList();

    // Last total size value sent to the client
    private int assumedSize;

    private boolean resendEntireRange = true;
    private boolean assumeEmptyClient = true;

    private int nextUpdateId = 0;

    // Keys that can be discarded once some specific update id gets confirmed
    private final HashMap<Integer, Set<String>> passivatedByUpdate = new HashMap<>();

    // Update ids that have been confirmed since the last flush
    private final HashSet<Integer> confirmedUpdates = new HashSet<>();

    private DataProvider<T, ?> dataProvider = DataProvider.ofItems();

    // Serializability of filter is up to the application
    private Object filter;
    private SerializableComparator<T> inMemorySorting;

    private final ArrayList<QuerySortOrder> backEndSorting = new ArrayList<>();

    private Registration dataProviderUpdateRegistration;
    private HashSet<T> updatedData = new HashSet<>();

    private SerializableConsumer<ExecutionContext> flushRequest;
    private SerializableConsumer<ExecutionContext> flushUpdatedDataRequest;

    private static class SizeVerifier<T> implements Consumer<T>, Serializable {

        private int size;

        private final int limit;

        private SizeVerifier(int limit) {
            this.limit = limit;
        }

        @Override
        public void accept(T t) {
            size++;
            if (size > limit) {
                throw new IllegalStateException(String.format(
                        "The number of items returned by "
                                + "the data provider exceeds the limit specified by the query (%d).",
                        limit));
            }
        }

    }

    /**
     * Creates a new instance.
     *
     * @param dataGenerator
     *            the data generator function
     * @param arrayUpdater
     *            array updater strategy
     * @param dataUpdater
     *            data updater strategy
     * @param stateNode
     *            the state node used to communicate for
     */
    public DataCommunicator(DataGenerator<T> dataGenerator,
            ArrayUpdater arrayUpdater,
            SerializableConsumer<JsonArray> dataUpdater, StateNode stateNode) {
        this.dataGenerator = dataGenerator;
        this.arrayUpdater = arrayUpdater;
        this.dataUpdater = dataUpdater;
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
        dataGenerator.destroyAllData();
        updatedData.clear();
        requestFlush();
    }

    /**
     * Informs the DataCommunicator that a data object has been updated.
     *
     * @param data
     *            updated data object; not {@code null}
     */
    public void refresh(T data) {
        Objects.requireNonNull(data,
                "DataCommunicator can not refresh null object");
        getKeyMapper().refresh(data);
        dataGenerator.refreshData(data);
        updatedData.add(data);
        requestFlushUpdatedData();
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

        handleDetach();

        reset();
        getKeyMapper().removeAll();

        this.dataProvider = dataProvider;

        getKeyMapper().setIdentifierGetter(dataProvider::getId);

        handleAttach();

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
     * Sets the {@link DataKeyMapper} used in this {@link DataCommunicator}. Key
     * mapper can be used to map keys sent to the client-side back to their
     * respective data objects.
     * 
     * @param keyMapper
     *            the keyMapper
     */
    protected void setKeyMapper(DataKeyMapper<T> keyMapper) {
        this.keyMapper = keyMapper;
    }

    /**
     * Sets the {@link Comparator} to use with in-memory sorting.
     *
     * @param comparator
     *            comparator used to sort data
     */
    public void setInMemorySorting(SerializableComparator<T> comparator) {
        inMemorySorting = comparator;
        reset();
    }

    /**
     * Returns the {@link Comparator} to use with in-memory sorting.
     *
     * @return comparator used to sort data
     */
    public SerializableComparator<T> getInMemorySorting() {
        return inMemorySorting;
    }

    /**
     * Sets the {@link QuerySortOrder}s to use with backend sorting.
     *
     * @param sortOrder
     *            list of sort order information to pass to a query
     */
    public void setBackEndSorting(List<QuerySortOrder> sortOrder) {
        backEndSorting.clear();
        backEndSorting.addAll(sortOrder);
        reset();
    }

    /**
     * Returns the {@link QuerySortOrder} to use with backend sorting.
     *
     * @return an unmodifiable list of sort order information to pass to a query
     */
    public List<QuerySortOrder> getBackEndSorting() {
        return Collections.unmodifiableList(backEndSorting);
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
        QueryTrace query = new QueryTrace(offset, limit, backEndSorting,
                inMemorySorting, filter);
        Stream<T> stream = getDataProvider().fetch(query);
        if (stream.isParallel()) {
            LoggerFactory.getLogger(DataCommunicator.class)
                    .debug("Data provider {} has returned "
                            + "parallel stream on 'fetch' call",
                            getDataProvider().getClass());
            stream = stream.collect(Collectors.toList()).stream();
            assert !stream.isParallel();
        }
        SizeVerifier verifier = new SizeVerifier<>(limit);
        stream = stream.peek(verifier);

        if (!query.isLimitCalled()) {
            throw new IllegalStateException(
                    getInvalidContractMessage("getLimit"));
        }
        if (!query.isOffsetCalled()) {
            throw new IllegalStateException(
                    getInvalidContractMessage("getOffset"));
        }
        return stream;
    }

    private String getInvalidContractMessage(String method) {
        return String.format("The data provider hasn't ever called %s() "
                + "method on the provided query. "
                + "It means that the the data provider breaks the contract "
                + "and the returned stream contains unxpected data.", method);
    }

    private void handleAttach() {
        dataProviderUpdateRegistration = getDataProvider()
                .addDataProviderListener(event -> {
                    if (event instanceof DataRefreshEvent) {
                        handleDataRefreshEvent((DataRefreshEvent<T>) event);
                    } else {
                        reset();
                    }
                });

        // Ensure the initialize check is done
        requestFlush();
    }

    protected void handleDataRefreshEvent(DataRefreshEvent<T> event) {
        refresh (event.getItem());
    }

    private void handleDetach() {
        dataGenerator.destroyAllData();
        if (dataProviderUpdateRegistration != null) {
            dataProviderUpdateRegistration.remove();
            dataProviderUpdateRegistration = null;
        }
    }

    private void requestFlush() {
        if (flushRequest == null) {
            flushRequest = context -> {
                if (!context.isClientSideInitialized()) {
                    reset();
                    arrayUpdater.initialize();
                }
                flush();
                flushRequest = null;
            };
            stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                    .beforeClientResponse(stateNode, flushRequest));
        }
    }

    private void requestFlushUpdatedData() {
        if (flushUpdatedDataRequest == null) {
            flushUpdatedDataRequest = context -> {
                flushUpdatedData();
                flushUpdatedDataRequest = null;
            };
            stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                    .beforeClientResponse(stateNode, flushUpdatedDataRequest));
        }
    }

    private void flush() {
        Set<String> oldActive = new HashSet<>(activeKeyOrder);

        Range effectiveRequested;
        final Range previousActive = Range.withLength(activeStart,
                activeKeyOrder.size());

        // Phase 1: Find all items that the client should have
        if (resendEntireRange) {
            assumedSize = getDataProviderSize();
        }
        effectiveRequested = requestedRange
                .restrictTo(Range.withLength(0, assumedSize));

        resendEntireRange |= !(previousActive.intersects(effectiveRequested)
                || (previousActive.isEmpty() && effectiveRequested.isEmpty()));

        Activation activation = collectKeysToFlush(previousActive,
                effectiveRequested);

        // If the returned stream from the DataProvider is smaller than it
        // should, a new query for the actual size needs to be done
        if (activation.isSizeRecheckNeeded()) {
            assumedSize = getDataProviderSize();
            effectiveRequested = requestedRange
                    .restrictTo(Range.withLength(0, assumedSize));
        }

        activeKeyOrder = activation.getActiveKeys();
        activeStart = effectiveRequested.getStart();

        // Phase 2: Collect changes to send
        Update update = arrayUpdater.startUpdate(assumedSize);
        boolean updated = collectChangesToSend(previousActive,
                effectiveRequested, update);

        resendEntireRange = false;
        assumeEmptyClient = false;

        // Phase 3: passivate anything that isn't longer active
        passivateInactiveKeys(oldActive, update, updated);

        // Phase 4: unregister passivated and updated items
        unregisterPassivatedKeys();
    }

    private void flushUpdatedData() {
        if (updatedData.isEmpty()) {
            return;
        }
        dataUpdater.accept(updatedData.stream().map(this::generateJson)
                .collect(JsonUtils.asArray()));
        updatedData.clear();
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
            passivated.forEach(key -> {
                T item = keyMapper.get(key);
                if (item != null) {
                    dataGenerator.destroyData(item);
                    keyMapper.remove(item);
                }
            });
        }
    }

    private void passivateInactiveKeys(Set<String> oldActive, Update update,
            boolean updated) {
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
            oldActive.removeAll(activeKeyOrder);
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

    private Activation collectKeysToFlush(final Range previousActive,
            final Range effectiveRequested) {
        /*
         * Collecting all items even though only some small sub range would
         * actually be useful can be optimized away once we have some actual
         * test coverage for the logic here.
         */
        if (resendEntireRange) {
            return activate(effectiveRequested);
        } else {
            List<String> newActiveKeyOrder = new ArrayList<>();
            boolean sizeRecheckNeeded = false;

            Range[] partitionWith = effectiveRequested
                    .partitionWith(previousActive);

            Activation activation = activate(partitionWith[0]);
            newActiveKeyOrder.addAll(activation.getActiveKeys());
            sizeRecheckNeeded |= activation.isSizeRecheckNeeded();

            // Pick existing items from the current list
            Range overlap = partitionWith[1].offsetBy(-activeStart);
            newActiveKeyOrder.addAll(activeKeyOrder.subList(overlap.getStart(),
                    overlap.getEnd()));

            activation = activate(partitionWith[2]);
            newActiveKeyOrder.addAll(activation.getActiveKeys());
            sizeRecheckNeeded |= activation.isSizeRecheckNeeded();
            return new Activation(newActiveKeyOrder, sizeRecheckNeeded);
        }
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

    private Activation activate(Range range) {
        if (range.isEmpty()) {
            return Activation.empty();
        }

        // XXX Explicitly refresh anything that is updated
        List<String> activeKeys = new ArrayList<>(range.length());
        fetchFromProvider(range.getStart(), range.length()).forEach(bean -> {
            boolean mapperHasKey = keyMapper.has(bean);
            String key = keyMapper.key(bean);
            if (mapperHasKey) {
                // Ensure latest instance from provider is used
                keyMapper.refresh(bean);
                passivatedByUpdate.values().stream()
                        .forEach(set -> set.remove(key));
            }
            activeKeys.add(key);
        });
        boolean needsSizeRecheck = activeKeys.size() < range.length();
        return new Activation(activeKeys, needsSizeRecheck);
    }

    private JsonValue generateJson(T item) {
        JsonObject json = Json.createObject();
        json.put("key", getKeyMapper().key(item));
        dataGenerator.generateData(item, json);
        return json;
    }

    private static class Activation implements Serializable {
        private final List<String> activeKeys;
        private final boolean sizeRecheckNeeded;

        public Activation(List<String> activeKeys, boolean sizeRecheckNeeded) {
            this.activeKeys = activeKeys;
            this.sizeRecheckNeeded = sizeRecheckNeeded;
        }

        public List<String> getActiveKeys() {
            return activeKeys;
        }

        public boolean isSizeRecheckNeeded() {
            return sizeRecheckNeeded;
        }

        public static Activation empty() {
            return new Activation(Collections.emptyList(), false);
        }
    }
}
