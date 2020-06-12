/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.flow.dom.Element;
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
import static org.slf4j.LoggerFactory.getLogger;

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
    private int lastSent = -1;

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

    private CallbackDataProvider.CountCallback<T, ?> sizeCallback;
    private SizeEstimateCallback<T, ?> sizeEstimateCallback;
    private int initialSizeEstimate = -1;
    private boolean definedSize = true;
    private boolean skipSizeCheckUntilReset;
    private boolean sizeReset;
    private int pageSize;
    // Using default value of 4 pages because some times grid wants initially
    // 150 rows -> defaulting to 4*50=200 as default size prevents flickering...
    private int sizeIncreasePageCount = 4;

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
        skipSizeCheckUntilReset = false;
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
     * <p>
     * This method also sets the data communicator to defined size - meaning
     * that the given data provider is queried for size and previous size
     * estimates are discarded.
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
        doSetDefinedSize(null, null, -1, true);
        skipSizeCheckUntilReset = false;
        sizeReset = false; // everything is cleared anyway

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
     * This is the latest DataProvider size informed to the client or fetched
     * from the DataProvider if client data has not been sent.
     *
     * @return size of available data
     */
    public int getDataSize() {
        if (resendEntireRange || assumeEmptyClient) {
            return getDataProviderSize();
        }
        return assumedSize;
    }

    /**
     * Get the active keys and order on active data on the client.
     *
     * @return list of active client data as ordered keys
     */
    public List<String> getActiveKeyOrdering() {
        return Collections.unmodifiableList(activeKeyOrder);
    }

    /**
     * Get the current client item range.
     *
     * @return range of items on client
     */
    public Range getRequestedRange() {
        return requestedRange;
    }

    /**
     * Generate a data query with component sorting and filtering.
     *
     * @param offset
     *            first index to fetch
     * @param limit
     *            fetched item count
     * @return {@link Query} for component state
     */
    public Query buildQuery(int offset, int limit) {
        return new Query(offset, limit, getBackEndSorting(),
                getInMemorySorting(), getFilter());
    }

    /**
     * Sets the page size that is used to fetch items. The queries to data
     * provider are a multiple of the page size.
     * 
     * @param pageSize
     *            the page size to set
     */
    // TODO add API that will force the communicator to only fetch one page at a
    // time from the data provider. Now it might be a multiple.
    public void setPageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException(String.format(
                    "Page size cannot be less than 1, got %s", pageSize));
        }
        this.pageSize = pageSize;
    }

    /**
     * Sets the number of pages that the component estimated size is increased
     * when the end has been reached with but when no
     * {@link #setSizeEstimateCallback(SizeEstimateCallback)} is provided. This
     * method is meant for component specific customizations, but not for
     * exposing in the component.
     *
     * @param sizeIncreasePageCount
     *            the count of pages to increase the size
     */
    public void setSizeIncreasePageCount(int sizeIncreasePageCount) {
        this.sizeIncreasePageCount = sizeIncreasePageCount;
    }

    /**
     * Returns the number of pages that the size is increased when the estimated
     * end has been reached in undefined size mode.
     */
    public int getSizeIncreasePageCount() {
        return sizeIncreasePageCount;
    }

    /**
     * Sets the size callback to be used and switches the component to defined
     * size. Any previously set callbacks or initial size are cleared. The new
     * size will be used after this roundtrip.
     * 
     * @param sizeCallback
     *            the size callback to use
     */
    public void setSizeCallback(
            CallbackDataProvider.CountCallback<T, ?> sizeCallback) {
        if (sizeCallback == null) {
            throw new IllegalArgumentException(
                    "Provided size callback cannot be null - for switching between defined and undefined size use setDefinedSize(boolean) method instead.");
        }
        doSetDefinedSize(sizeCallback, null, -1, true);
        skipSizeCheckUntilReset = false;
        sizeReset = true;
        requestFlush();
    }

    /**
     * Sets the size estimate callback to be used and switches teh component to
     * undefined size. Any previously set callbacks or initial size are cleared.
     * The new estimate will be fetched after this roundtrip.
     * 
     * @param sizeEstimateCallback
     *            the size estimate callback to use
     */
    public void setSizeEstimateCallback(
            SizeEstimateCallback<T, ?> sizeEstimateCallback) {
        if (sizeEstimateCallback == null) {
            throw new IllegalArgumentException(
                    "Provided size estimate callback cannot be null - for switching between defined and undefined size use setDefinedSize(boolean) method instead.");
        }
        doSetDefinedSize(null, sizeEstimateCallback, -1, false);
        if (!skipSizeCheckUntilReset) {
            sizeReset = true;
            requestFlush();
        }
    }

    /**
     * Sets the initial size estimate to use and switches component to undefined
     * size. Any previously set size related callbacks are cleared. The new
     * estimate is only applied if it is greater than the currently
     * estimated/known size. Otherwise it is not applied until there has been a
     * reset.
     * 
     * @param initialSizeEstimate
     *            the initial size estimate to be used
     */
    public void setInitialSizeEstimate(int initialSizeEstimate) {
        if (initialSizeEstimate < 1) {
            throw new IllegalArgumentException(
                    "Given initial size estimate cannot be less than 1. For switching between defined and undefined size use setDefinedSize(boolean) method instead.");
        }
        doSetDefinedSize(null, null, initialSizeEstimate, false);
        if (!skipSizeCheckUntilReset && assumedSize < initialSizeEstimate) {
            sizeReset = true;
            requestFlush();
        }
    }

    /**
     * Changes between defined and undefined size and clears any previously set
     * size and size estimate callbacks and initial size estimate. Thus calling
     * with value {@code true} will use the {@link DataProvider#size(Query)} for
     * getting the size. Calling with {@code false} will use the default initial
     * size and increase the size by {@link #setPageSize(int)} multiplied with
     * {@link #setSizeIncreasePageCount(int)}.
     * 
     * @param definedSize
     *            {@code true} for defined size, {@code false} for undefined
     *            size
     */
    public void setDefinedSize(boolean definedSize) {
        if (this.definedSize != definedSize) {
            doSetDefinedSize(null, null, -1, definedSize);
            skipSizeCheckUntilReset = false;
            if (definedSize) {
                // Always fetch explicit size from data provider
                requestFlush();
            } else
            /*
             * Only do a new estimate if scrolled to end to increase the
             * estimated size. If there was a previous defined size used, then
             * that is kept until a reset occurs.
             */
            if (requestedRange.contains(assumedSize)) {
                requestFlush();
            }
        }
    }

    /**
     * Returns whether defined or undefined size is used.
     * 
     * @return {@code true} for defined size, {@code false} for undefined size
     */
    public boolean isDefinedSize() {
        return definedSize;
    }

    private void doSetDefinedSize(
            CallbackDataProvider.CountCallback<T, ?> sizeCallback,
            SizeEstimateCallback<T, ?> sizeEstimateCallback,
            int initialSizeEstimate, boolean definedSize) {
        this.sizeCallback = sizeCallback;
        this.sizeEstimateCallback = sizeEstimateCallback;
        this.initialSizeEstimate = initialSizeEstimate;
        this.definedSize = definedSize;
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
     * Getter method for determining the initial size of the data. Can be
     * overridden by a subclass that uses a specific type of DataProvider and/or
     * query.
     *
     * @return the size of data provider with current filter
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected int getDataProviderSize() {
        assert definedSize : "This method should never be called when using undefined size";
        if (sizeCallback != null) {
            return sizeCallback.count(new Query(getFilter()));
        } else {
            return getDataProvider().size(new Query(getFilter()));
        }
    }

    private void updateUndefinedSize() {
        assert !definedSize : "This method should never be called when using defined size";
        // things have reset
        if (resendEntireRange || sizeReset) {
            // 1. given size estimate
            int size = initialSizeEstimate;
            // 2. given estimate callback
            if (sizeEstimateCallback != null) {
                size = getNewSizeEstimateFromCallback(true);
            }
            // 3. default initial size
            if (size == -1) {
                size = getDefaultInitialSize();
            }
            getLogger(DataCommunicator.class)
                    .info("Requested: " + requestedRange + " old size: "
                            + assumedSize + " new size: " + size);
            assumedSize = size;
        } else {
            // increase size estimate if the last page is being fetched
            if (requestedRange.getEnd() + pageSize > assumedSize) {
                int previousAssumedSize = assumedSize;
                if (sizeEstimateCallback != null) {
                    assumedSize = getNewSizeEstimateFromCallback(false);
                } else {
                    // by default adjust size by multiple of page size
                    assumedSize += getEstimatedSizeIncrease();
                }
                getLogger(DataCommunicator.class).info("Requested: "
                        + requestedRange + " old size: " + previousAssumedSize
                        + " new size: " + assumedSize);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private SizeEstimateQuery buildSizeEstimateQuery(boolean reset) {
        int offset = -1;
        int limit = -1;
        // not sure if something else could be given, requestedRange defaults to
        // 0 -> 0
        if (requestedRange != null) {
            offset = requestedRange.getStart();
            limit = requestedRange.length();
        }
        return new SizeEstimateQuery(offset, limit, getBackEndSorting(),
                getInMemorySorting(), getFilter(), assumedSize, reset);
    }

    @SuppressWarnings("unchecked")
    private int getNewSizeEstimateFromCallback(boolean reset) {
        assert sizeEstimateCallback != null : "Cannot get estimated size if no callback set";

        int size = sizeEstimateCallback
                .sizeEstimate(buildSizeEstimateQuery(reset));

        if (size < 0) {
            throw new IllegalStateException(String.format(
                    "Size cannot be estimated to be negative, got %s", size));
        }
        /*
         * Using an estimate that is less than the requested range would lead
         * into grid not scrolling anymore, so not allowing that for now as it
         * is not the desired behavior. It is better to request for too many
         * items and then get less than try to get an exact estimate.
         */
        if (!reset && size <= requestedRange.getEnd()) {
            throw new IllegalStateException(String.format(
                    "Estimated size %s is less than the requested range %s, "
                            + "this is not allowed with undefined size. "
                            + "The new estimated size should surpass the range the component has requested (offset + limit).",
                    size, requestedRange.getEnd()));
        }
        return size;
    }

    private int getEstimatedSizeIncrease() {
        return pageSize * sizeIncreasePageCount;
    }

    private int getDefaultInitialSize() {
        // for now using the same initial size as the buffer page increase
        return pageSize * sizeIncreasePageCount;
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
            getLogger(DataCommunicator.class).debug(
                    "Data provider {} has returned "
                            + "parallel stream on 'fetch' call",
                    getDataProvider().getClass());
            stream = stream.collect(Collectors.toList()).stream();
            assert !stream.isParallel();
        }

        SizeVerifier verifier = new SizeVerifier<>(limit);
        stream = stream.peek(verifier);

        // FIXME simplify by removing these restrictions ?
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
        refresh(event.getItem());
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

        // With defined size the backend is only queried when necessary
        if (definedSize && (resendEntireRange || sizeReset)) {
            assumedSize = getDataProviderSize();
        } else if (!definedSize && (!skipSizeCheckUntilReset || sizeReset)) {
            // with undefined size, size estimate is checked when scrolling down
            updateUndefinedSize();
        }
        effectiveRequested = requestedRange
                .restrictTo(Range.withLength(0, assumedSize));

        resendEntireRange |= !(previousActive.intersects(effectiveRequested)
                || (previousActive.isEmpty() && effectiveRequested.isEmpty()));

        Activation activation = collectKeysToFlush(previousActive,
                effectiveRequested);

        // In case received less items than what was expected, adjust size
        if (activation.isSizeRecheckNeeded()) {
            if (definedSize) {
                assumedSize = getDataProviderSize();
            } else {
                // the end has been reached
                assumedSize = requestedRange.getStart()
                        + activation.getActiveKeys().size();
                skipSizeCheckUntilReset = true;
            }
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
        sizeReset = false;

        // Phase 3: passivate anything that isn't longer active
        passivateInactiveKeys(oldActive, update, updated);

        // Phase 4: unregister passivated and updated items
        unregisterPassivatedKeys();

        fireSizeEvent(assumedSize);
    }

    /**
     * Fire a size change event if the last event was fired for a different size
     * from the last sent one.
     *
     * @param dataSize
     *            data size to send
     */
    private void fireSizeEvent(int dataSize) {
        if (lastSent != dataSize) {
            final Optional<Component> component = Element.get(stateNode)
                    .getComponent();
            if (component.isPresent()) {
                ComponentUtil.fireEvent(component.get(),
                        new SizeChangeEvent<>(component.get(), dataSize));
            }
            lastSent = dataSize;
        }
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
