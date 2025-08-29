/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.NullOwner;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;

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
    public static final int DEFAULT_PAGE_INCREASE_COUNT = 4;

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAXIMUM_ALLOWED_PAGES = 10;
    private static final int MAXIMUM_ALLOWED_ITEMS_LOWER_LIMIT = 500;

    private final DataGenerator<T> dataGenerator;
    private final ArrayUpdater arrayUpdater;
    private final SerializableConsumer<JsonArray> dataUpdater;
    private final StateNode stateNode;

    // Keys that can be discarded once some specific update id gets confirmed
    protected final HashMap<Integer, Set<String>> passivatedByUpdate = new HashMap<>();

    // Update ids that have been confirmed since the last flush
    private final HashSet<Integer> confirmedUpdates = new HashSet<>();

    private final ArrayList<QuerySortOrder> backEndSorting = new ArrayList<>();

    private DataKeyMapper<T> keyMapper = new KeyMapper<>();

    // The range of items that the client wants to have
    private Range viewportRange = Range.between(0, 0);

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

    private DataProvider<T, ?> dataProvider = new EmptyDataProvider<>();

    private Filter<?> filter;
    private SerializableComparator<T> inMemorySorting;

    private Registration dataProviderUpdateRegistration;
    private HashSet<T> updatedData = new HashSet<>();
    private FlushRequest flushRequest;
    private FlushRequest flushUpdatedDataRequest;
    private boolean flushInProgress = false;
    private boolean flushUpdatedDataInProgress = false;

    private CallbackDataProvider.CountCallback<T, ?> countCallback;
    private int itemCountEstimate = -1;
    private int itemCountEstimateIncrease = -1;
    private boolean definedSize = true;
    private boolean skipCountIncreaseUntilReset;
    private boolean sizeReset;
    private int pageSize = DEFAULT_PAGE_SIZE;

    // Paged queries are enabled by default
    private boolean pagingEnabled = true;

    private boolean fetchEnabled;

    private transient Executor executor = null;
    private transient CompletableFuture<Activation> future;

    /**
     * In-memory data provider with no items.
     * <p>
     * Data Communicator is initialised with this data provider by default until
     * a new data provider is assigned with
     * {@link #setDataProvider(DataProvider, Object)}.
     *
     * @param <T1>
     *            item type
     *
     * @see AbstractDataView#AbstractDataView(SerializableSupplier, Component)
     */
    public static final class EmptyDataProvider<T1>
            extends ListDataProvider<T1> {
        /**
         * Create in-memory data provider instance with no items in the backed
         * collection.
         */
        public EmptyDataProvider() {
            super(new ArrayList<>(0));
        }
    }

    /**
     * Wraps the component's filter object with the meta information whether
     * this filter changing should trigger the item count change event.
     *
     * @param <F>
     *            filter's type
     */
    public static final class Filter<F> implements Serializable {

        // Serializability of filter is up to the application
        private F filterObject;

        private boolean notifyOnChange;

        /**
         * Creates the filter object and sets it notify item count change
         * listeners by default.
         *
         * @param filterObject
         *            filter object of a component
         */
        public Filter(F filterObject) {
            this.filterObject = filterObject;
            this.notifyOnChange = true;
        }

        /**
         * Creates the filter object and sets its lifespan.
         *
         * @param filterObject
         *            filter object of a component
         * @param notifyOnChange
         *            if {@code true}, then the data communicator will fire the
         *            item count change event as soon as filter change modifies
         *            the item count. If {@code false}, the item count change
         *            event won't be fired, even if the item count will be
         *            changed as a result of filtering.
         */
        public Filter(F filterObject, boolean notifyOnChange) {
            this.filterObject = filterObject;
            this.notifyOnChange = notifyOnChange;
        }

        /**
         * Returns a filter object for this component.
         *
         * @return filter object
         */
        public F getFilterObject() {
            return filterObject;
        }

        /**
         * Returns whether to fire the item change event or not upon filter
         * changing.
         *
         * @return {@code true}, then the data communicator will fire the item
         *         count change event as soon as filter change modifies the item
         *         count. Returns {@code false}, the item count change event
         *         won't be fired, even if the item count will be changed as a
         *         result of filtering.
         */
        public boolean isNotifyOnChange() {
            return notifyOnChange;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Filter<?> filter1 = (Filter<?>) o;
            return notifyOnChange == filter1.notifyOnChange
                    && Objects.equals(filterObject, filter1.filterObject);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filterObject, notifyOnChange);
        }
    }

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
        this(dataGenerator, arrayUpdater, dataUpdater, stateNode, true);
    }

    /**
     * Creates a new instance.
     * <p>
     * Allows to setup whether the data communicator will ignore fetch and size
     * queries to data provider until further configuration. This mode is useful
     * when the component needs to postpone the calls to data provider until
     * some event, i.e. dropdown open event of the combo box, but needs to
     * configure the data communicator preliminary.
     *
     * @param dataGenerator
     *            the data generator function
     * @param arrayUpdater
     *            array updater strategy
     * @param dataUpdater
     *            data updater strategy
     * @param stateNode
     *            the state node used to communicate for
     * @param fetchEnabled
     *            if {@code fetchEnabled} is {@code true} then the data provider
     *            will be called to fetch the items and/or to get the items
     *            count until it's set to {@code false}
     */
    public DataCommunicator(DataGenerator<T> dataGenerator,
            ArrayUpdater arrayUpdater,
            SerializableConsumer<JsonArray> dataUpdater, StateNode stateNode,
            boolean fetchEnabled) {
        this.dataGenerator = dataGenerator;
        this.arrayUpdater = arrayUpdater;
        this.dataUpdater = dataUpdater;
        this.stateNode = stateNode;
        this.fetchEnabled = fetchEnabled;

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
     * @deprecated since 24.9 and will be removed in Vaadin 26. Use
     *             {@link #setViewportRange(int, int)} instead.
     */
    public void setRequestedRange(int start, int length) {
        viewportRange = computeViewportRange(start, length);
        requestFlush();
    }

    /**
     * Sets the range of data to be sent to the client.
     *
     * @param start
     *            the start of the viewport range
     * @param length
     *            the end of the viewport range
     */
    public void setViewportRange(int start, int length) {
        setRequestedRange(start, length);
    }

    /**
     * Computes the requested range, limiting the number of requested items to a
     * given threshold of ten pages.
     *
     * @param start
     *            the start of the requested range
     * @param length
     *            the end of the requested range
     * @return
     * @deprecated since 24.9 and will be removed in Vaadin 26. Use
     *             {@link #computeViewportRange(int, int)} instead.
     */
    protected final Range computeRequestedRange(int start, int length) {
        final int maximumAllowedItems = getMaximumAllowedItems();
        if (length > maximumAllowedItems) {
            getLogger().warn(String.format(
                    "Attempted to fetch more items from server than allowed "
                            + "in one go: number of items requested '%d', maximum "
                            + "items allowed '%d'.",
                    length, maximumAllowedItems));
        }
        return Range.withLength(start, Math.min(length, maximumAllowedItems));
    }

    /**
     * Computes the viewport range, limiting the number of requested items to a
     * given threshold of ten pages.
     *
     * @param start
     *            the start of the viewport range
     * @param length
     *            the end of the viewport range
     */
    protected final Range computeViewportRange(int start, int length) {
        return computeRequestedRange(start, length);
    }

    /**
     * Control whether DataCommunicator should push data updates to the
     * component asynchronously or not. By default the executor service is not
     * defined and updates are done synchronously. Setting to null will disable
     * the feature.
     * <p>
     * Note: This works only with Grid component. If set to true, Push needs to
     * be enabled and set to PushMode.AUTOMATIC in order this to work.
     *
     * @param executor
     *            The Executor used for async updates.
     */
    public void enablePushUpdates(Executor executor) {
        if (this.executor != null && future != null) {
            future.cancel(true);
            future = null;
        }
        this.executor = executor;
    }

    /**
     * Resets all the data.
     * <p>
     * It effectively resends all available data.
     */
    public void reset() {
        skipCountIncreaseUntilReset = false;
        sizeReset = true;
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
     * Schedules a re-render of the items that are currently in the viewport to
     * refresh their content with the latest data from the generators.
     */
    protected void refreshViewport() {
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

        // Release the memory for confirmed updates
        unregisterPassivatedKeys();
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
     * <p>
     * This method allows to define whether the data communicator notifies about
     * changing of item count when it changes due to filtering.
     *
     * @param dataProvider
     *            the data provider to set, not <code>null</code>
     * @param initialFilter
     *            the initial filter value to use, or <code>null</code> to not
     *            use any initial filter value
     * @param notifiesOnChange
     *            if {@code true}, then the data communicator will fire the item
     *            count change event as soon as filter change modifies the item
     *            count. If {@code false}, the item count change event won't be
     *            fired, even if the item count will be changed as a result of
     *            filtering.
     *
     * @param <F>
     *            the filter type
     *
     * @return a consumer that accepts a new filter value to use
     */
    public <F> SerializableConsumer<Filter<F>> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter,
            boolean notifiesOnChange) {
        Objects.requireNonNull(dataProvider, "data provider cannot be null");

        removeFilteringAndSorting();

        filter = initialFilter != null
                ? new Filter<>(initialFilter, notifiesOnChange)
                : null;

        countCallback = null;
        definedSize = true;
        sizeReset = true;

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
     * @param <F>
     *            the filter type
     *
     * @return a consumer that accepts a new filter value to use
     */
    public <F> SerializableConsumer<F> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter) {
        SerializableConsumer<Filter<F>> filterConsumer = setDataProvider(
                dataProvider, initialFilter, true);
        return newFilter -> filterConsumer.accept(new Filter<>(newFilter));
    }

    /**
     * This is the latest DataProvider item count informed to the client or
     * fetched from the DataProvider if client data has not been sent.
     *
     * @return count of available items
     */
    public int getItemCount() {
        if (isDefinedSize()
                && (resendEntireRange || assumeEmptyClient || sizeReset)) {
            // TODO it could be possible to cache the value returned here
            // and use it next time instead of making another query, unless
            // the conditions like filter (or another reset) have changed
            return getDataProviderSize();
        }
        // do not report a stale size or size estimate
        if (!isDefinedSize() && sizeReset) {
            return 0;
        }
        return assumedSize;
    }

    /**
     * Returns whether the given item is part of the active items.
     *
     * @param item
     *            the item to check, not {@code null}
     * @return {@code true} if item is active, {@code false} if not
     */
    public boolean isItemActive(T item) {
        return getKeyMapper().has(item);
    }

    /**
     * Gets the item at the given index from the data available to the
     * component. Data is filtered and sorted the same way as in the component.
     * <p>
     * Call to the backend is triggered if the item for a requested index is not
     * present in the cached active items.
     *
     * @param index
     *            the index of the item to get
     * @return item on index
     * @throws IndexOutOfBoundsException
     *             requested index is outside of the filtered and sorted data
     *             set
     */
    @SuppressWarnings("unchecked")
    public T getItem(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index must be non-negative");
        }
        int activeDataEnd = activeStart + activeKeyOrder.size() - 1;
        /*
         * Check if the item on a requested index is already in the cache of
         * active items. No matter is this currently a defined or undefined mode
         */
        if (index >= activeStart && index <= activeDataEnd) {
            return getKeyMapper().get(activeKeyOrder.get(index - activeStart));
        } else {
            final int itemCount = getItemCount();
            /*
             * The exception is thrown if the exact size is used and the data is
             * empty, or the index is outside of the item count range, because
             * we definitely know the item count from a backend.
             */
            if (isDefinedSize()) {
                if (itemCount == 0) {
                    throw new IndexOutOfBoundsException(String.format(
                            "Requested index %d on empty data.", index));
                } else if (index >= itemCount) {
                    throw new IndexOutOfBoundsException(String.format(
                            "Given index %d is outside of the accepted range '0 - %d'",
                            index, itemCount - 1));
                }
            }
            /*
             * In case of undefined size we don't check the empty data or the
             * item count, because item count = 0 may mean the flush (fetch)
             * action hasn't been made yet. And even if the requested index is
             * outside of the item count estimation, we can make the request,
             * because the backend can have the item on that index (we simply
             * not yet fetched this item during the scrolling).
             */
            try (Stream<T> stream = getDataProvider()
                    .fetch(buildQuery(index, 1))) {
                return stream.findFirst().orElse(null);
            }
        }
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
    public void setPageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException(String.format(
                    "Page size cannot be less than 1, got %d", pageSize));
        }
        this.pageSize = pageSize;
    }

    /**
     * Returns the page size set to fetch items.
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets the size callback to be used and switches the component to exact row
     * count. The new count will be used after this roundtrip.
     *
     * @param countCallback
     *            the size callback to use
     */
    public void setCountCallback(
            CallbackDataProvider.CountCallback<T, ?> countCallback) {
        if (countCallback == null) {
            throw new IllegalArgumentException(
                    "Provided size callback cannot be null - for switching "
                            + "between defined and undefined size use "
                            + "setDefinedSize(boolean) method instead.");
        }
        this.countCallback = countCallback;
        definedSize = true;
        skipCountIncreaseUntilReset = false;
        // there is no reset but we need to get the defined size
        sizeReset = true;
        requestFlush();
    }

    /**
     * Sets the item count estimate to use and switches component to undefined
     * size. Any previously set count callback is cleared. The new estimate is
     * applied if the actual count has not been discovered and if the estimate
     * is greater than the number of requested items. Otherwise it is not
     * applied until there has been a reset.
     * <p>
     * <em>NOTE:</em> setting item count estimate that is less than two pages
     * (set with {@link #setPageSize(int)}) can cause extra requests initially
     * or after a reset.
     *
     * @param itemCountEstimate
     *            the item count estimate to be used
     */
    public void setItemCountEstimate(int itemCountEstimate) {
        if (itemCountEstimate < 1) {
            throw new IllegalArgumentException(
                    "Given item count estimate cannot be less than 1.");
        }
        this.itemCountEstimate = itemCountEstimate;
        this.countCallback = null;
        definedSize = false;
        if (!skipCountIncreaseUntilReset
                && viewportRange.getEnd() < itemCountEstimate) {
            sizeReset = true;
            requestFlush();
        }
    }

    /**
     * Gets the item count estimate used.
     *
     * @return the item count estimate used
     */
    public int getItemCountEstimate() {
        if (itemCountEstimate < 1) {
            return pageSize * DEFAULT_PAGE_INCREASE_COUNT;
        }
        return itemCountEstimate;
    }

    /**
     * Sets the item count estimate increase to use and switches the component
     * to undefined size if not yet used. Any previously set count callback is
     * cleared. The step is used the next time that the count is adjusted.
     * <em>NOTE:</em> the increase should be greater than the
     * {@link #setPageSize(int)} or it may cause bad performance.
     *
     * @param itemCountEstimateIncrease
     *            the item count estimate step to use
     */
    public void setItemCountEstimateIncrease(int itemCountEstimateIncrease) {
        if (itemCountEstimateIncrease < 1) {
            throw new IllegalArgumentException(
                    "itemCountEstimateIncrease cannot be less than 1");
        }
        this.itemCountEstimateIncrease = itemCountEstimateIncrease;
        countCallback = null;
        definedSize = false;
    }

    /**
     * Gets the item count estimate increase used.
     *
     * @return the item count estimate increase
     */
    public int getItemCountEstimateIncrease() {
        if (itemCountEstimateIncrease == -1) {
            return pageSize * DEFAULT_PAGE_INCREASE_COUNT;
        } else {
            assert itemCountEstimate > 0 : "0 is not an increase";
            // might be sensible to force this to be a multiple of page size,
            // but being lenient for now
            return itemCountEstimateIncrease;
        }
    }

    /**
     * Changes between defined and undefined size and clears any previously set
     * count callback. Calling with value {@code true} will use the
     * {@link DataProvider#size(Query)} for getting the size. Calling with
     * {@code false} will use whatever has been set with
     * {@link #setItemCountEstimate(int)} and increase the count when needed
     * with {@link #setItemCountEstimateIncrease(int)}.
     *
     * @param definedSize
     *            {@code true} for defined size, {@code false} for undefined
     *            size
     */
    public void setDefinedSize(boolean definedSize) {
        if (this.definedSize != definedSize) {
            this.definedSize = definedSize;
            countCallback = null;
            skipCountIncreaseUntilReset = false;
            if (definedSize) {
                // Always fetch explicit count from data provider
                requestFlush();
            } else
            /*
             * Only do a new estimate if scrolled to end to increase the
             * estimated size. If there was a previous defined size used, then
             * that is kept until a reset occurs.
             */
            if (viewportRange.contains(assumedSize - 1)) {
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
     * Returns whether paged queries are enabled or not.
     * <p>
     * When the paged queries are supported, the {@link Query#getPage()} and
     * {@link Query#getPageSize()} can be used to fetch items from the paged
     * repositories. Otherwise, one should use {@link Query#getOffset()} and
     * {@link Query#getLimit()}. Paged queries are enabled by default.
     *
     * @return {@code true} for paged queries, {@code false} for offset/limit
     *         queries
     *
     * @see #setPagingEnabled(boolean)
     */
    public boolean isPagingEnabled() {
        return pagingEnabled;
    }

    /**
     * Sets whether paged queries or offset/limit queries will be used.
     *
     * @param pagingEnabled
     *            {@code true} for paged queries, {@code false} for offset/limit
     *            queries
     */
    public void setPagingEnabled(boolean pagingEnabled) {
        this.pagingEnabled = pagingEnabled;
    }

    /**
     * Returns whether the data communicator will call Data Provider for
     * fetching the items and/or getting the items count, or ignore such a
     * calls.
     *
     * @return {@code true} if the calls to data provider are enabled,
     *         {@code false} otherwise
     */
    public boolean isFetchEnabled() {
        return fetchEnabled;
    }

    /**
     * Sets whether the data communicator will call Data Provider for fetching
     * the items and/or getting the items count, or ignore such a calls.
     * <p>
     * One may need to disable the data provider calls in order to configure the
     * data communicator and to postpone these calls until some event, i.e.
     * dropdown open event of the combo box.
     * <p>
     * This sets to {@code true} by default.
     *
     * @param fetchEnabled
     *            if {@code true} then the calls to data provider are enabled,
     *            otherwise the data provider won't be called to fetch the
     *            items.
     */
    public void setFetchEnabled(boolean fetchEnabled) {
        this.fetchEnabled = fetchEnabled;
    }

    /**
     * Getter method for determining the item count of the data.
     * <p>
     * This method should be used only with defined size, i.e. when
     * {@link #isDefinedSize()} returns {@code true}.
     * <p>
     * Can be overridden by a subclass that uses a specific type of DataProvider
     * and/or query.
     *
     * @return the size of data provider with current filter
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int getDataProviderSize() {
        assert definedSize
                : "This method should never be called when using undefined size";
        if (countCallback != null) {
            return countCallback.count(new Query(getFilter()));
        } else {
            return getDataProvider().size(new Query(getFilter()));
        }
    }

    private void updateUndefinedSize() {
        assert !definedSize
                : "This method should never be called when using defined size";
        if (resendEntireRange || sizeReset) {
            // things have reset
            assumedSize = getItemCountEstimate();
        }

        // increase size estimate if the last page is being fetched,
        // or if the estimate is less than what is shown on client
        while (viewportRange.getEnd() + pageSize > assumedSize) {
            // by default adjust size by multiple of page size
            assumedSize += getItemCountEstimateIncrease();
        }
    }

    /**
     * Get the object used for filtering in this data communicator.
     *
     * @return the filter object of this data communicator
     */
    protected Object getFilter() {
        return filter != null ? filter.getFilterObject() : null;
    }

    /**
     * Fetches a list of items from the DataProvider.
     * <p>
     * <em>NOTE:</em> the {@code limit} parameter shows how many items the
     * client wants to fetch, but the actual number of results may be greater,
     * and vary from {@code 0 to pages * pageSize}.
     *
     * @param offset
     *            the starting index of the range
     * @param limit
     *            the desired number of results
     * @return the list of items in given range
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Stream<T> fetchFromProvider(int offset, int limit) {
        Stream<T> stream;

        if (pagingEnabled) {
            /*
             * Items limit value may not be necessarily multiply of page size,
             * and thus the pages count is rounded to closest smallest integer
             * in order to overlap the viewport range. Integer division is used
             * here for simplicity and to avoid double-int-double conversion.
             * Divisor minus one is placed on numerator part to ensure upwards
             * rounding.
             */
            final int pages = (limit - 1) / pageSize + 1;

            if (limit > pageSize) {
                /*
                 * Viewport range is split to several pages, and queried from
                 * backend page by page
                 */
                final Stream.Builder<T> streamBuilder = Stream.builder();

                final AtomicInteger fetchedPerPage = new AtomicInteger(0);
                Consumer<T> addItemAndCheckConsumer = item -> {
                    streamBuilder.add(item);
                    fetchedPerPage.getAndIncrement();
                };
                // Keep fetching the pages until we get empty/partial page,
                // or run out of pages to request
                int page = 0;
                do {
                    final int newOffset = offset + page * pageSize;
                    try (Stream<T> dataProviderStream = doFetchFromDataProvider(
                            newOffset, pageSize)) {
                        // Stream.Builder is not thread safe, so for parallel
                        // stream we need to first collect items before adding
                        // them
                        if (dataProviderStream.isParallel()) {
                            getLogger().debug(
                                    "Data provider {} has returned parallel stream on 'fetch' call",
                                    getDataProvider().getClass());
                            dataProviderStream.collect(Collectors.toList())
                                    .forEach(addItemAndCheckConsumer);
                        } else {
                            dataProviderStream.forEach(addItemAndCheckConsumer);
                        }
                    }
                    page++;
                } while (page < pages
                        && fetchedPerPage.getAndSet(0) == pageSize);

                stream = streamBuilder.build();
            } else {
                stream = doFetchFromDataProvider(offset, limit);
            }
            limit = pages * pageSize;
        } else {
            stream = doFetchFromDataProvider(offset, limit);
        }

        if (stream.isParallel()) {
            getLogger().debug(
                    "Data provider {} has returned parallel stream on 'fetch' call",
                    getDataProvider().getClass());
            try (Stream<T> parallelStream = stream) {
                stream = parallelStream.collect(Collectors.toList()).stream();
                assert !stream.isParallel();
            }
        }

        SizeVerifier verifier = new SizeVerifier<>(limit);
        return stream.peek(verifier);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Stream<T> doFetchFromDataProvider(int offset, int limitedTo) {
        QueryTrace query = new QueryTrace(offset, limitedTo, backEndSorting,
                inMemorySorting, getFilter());
        Stream<T> stream = getDataProvider().fetch(query);
        verifyQueryContract(query);
        return stream;
    }

    @SuppressWarnings("rawtypes")
    private void verifyQueryContract(QueryTrace query) {
        /*
         * These restrictions are used to help users to see that they have done
         * a mistake instead of just letting things work in an unintended way.
         */
        if (!query.isLimitCalled()) {
            throw new IllegalStateException(
                    getInvalidContractMessage("getLimit() or getPageSize()"));
        }
        if (!query.isOffsetCalled()) {
            throw new IllegalStateException(
                    getInvalidContractMessage("getOffset() or getPage()"));
        }
    }

    private String getInvalidContractMessage(String method) {
        return String.format("The data provider hasn't ever called %s "
                + "method on the provided query. "
                + "It means that the the data provider breaks the contract "
                + "and the returned stream contains unexpected data.", method);
    }

    private void handleAttach() {
        if (dataProviderUpdateRegistration != null) {
            dataProviderUpdateRegistration.remove();
        }

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
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        dataGenerator.destroyAllData();
        if (dataProviderUpdateRegistration != null) {
            dataProviderUpdateRegistration.remove();
            dataProviderUpdateRegistration = null;
        }
    }

    private void requestFlush() {
        requestFlush(false);
    }

    private void requestFlush(boolean forced) {
        if (!shouldRequestFlush(forced)) {
            return;
        }
        flushRequest = FlushRequest.register(stateNode, context -> {
            flushInProgress = true;
            try {
                if (!context.isClientSideInitialized()) {
                    reset();
                    arrayUpdater.initialize();
                }
                flush();
            } finally {
                flushRequest = null;
                flushInProgress = false;
            }
        });
    }

    private boolean shouldRequestFlush(boolean forced) {
        if (!fetchEnabled) {
            return false;
        }
        if (forced) {
            return true;
        }
        // New requests that are not forced are not registered while a flush
        // is in progress. This prevents infinite loop in cases including
        // @PreserveOnRefresh.
        return !flushInProgress && (flushRequest == null
                || !flushRequest.canExecute(stateNode));
    }

    private void requestFlushUpdatedData() {
        if (!shouldRequestFlushUpdatedData()) {
            return;
        }
        flushUpdatedDataRequest = FlushRequest.register(stateNode, context -> {
            flushUpdatedDataInProgress = true;
            try {
                flushUpdatedData();
            } finally {
                flushUpdatedDataRequest = null;
                flushUpdatedDataInProgress = false;
            }
        });
    }

    private boolean shouldRequestFlushUpdatedData() {
        // New requests are not registered while a flush is in progress. This
        // prevents infinite loop in cases including @PreserveOnRefresh.
        return !flushUpdatedDataInProgress && (flushUpdatedDataRequest == null
                || !flushUpdatedDataRequest.canExecute(stateNode));
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
        } else if (!definedSize
                && (!skipCountIncreaseUntilReset || sizeReset)) {
            // with undefined size, size estimate is checked when scrolling down
            updateUndefinedSize();
        }
        effectiveRequested = viewportRange
                .restrictTo(Range.withLength(0, assumedSize));

        resendEntireRange |= !(previousActive.intersects(effectiveRequested)
                || (previousActive.isEmpty() && effectiveRequested.isEmpty()));

        UI ui = getUI();
        if (ui != null && executor != null) {
            // In async mode wrap fetching data in future, collectKeysToFlush
            // will perform fetch from data provider with given range.
            if (ui.getPushConfiguration().getPushMode() != PushMode.AUTOMATIC) {
                throw new IllegalStateException(
                        "Asynchronous DataCommunicator updates require Push to be enabled and PushMode.AUTOMATIC");
            }
            if (future != null) {
                future.cancel(true);
            }
            future = CompletableFuture
                    .supplyAsync(() -> collectKeysToFlush(previousActive,
                            effectiveRequested), executor);
            future.thenAccept(activation -> {
                if (ui == null) {
                    return;
                }
                ui.access(() -> {
                    performUpdate(oldActive, effectiveRequested, previousActive,
                            activation);
                });
            });
        } else {

            Activation activation = collectKeysToFlush(previousActive,
                    effectiveRequested);

            performUpdate(oldActive, effectiveRequested, previousActive,
                    activation);
        }
    }

    private void performUpdate(Set<String> oldActive, Range effectiveRequested,
            final Range previousActive, Activation activation) {
        // In case received less items than what was expected, adjust size
        if (activation.isSizeRecheckNeeded()) {
            if (definedSize) {
                assumedSize = getDataProviderSize();
            } else {
                // the end has been reached
                assumedSize = viewportRange.getStart()
                        + activation.getActiveKeys().size();
                skipCountIncreaseUntilReset = true;
                /*
                 * If the fetch query returned 0 items, it means that the user
                 * has scrolled past the end of the exact item count or the
                 * items have been changed in the backend (for example, applying
                 * the filter). Instead of returning 0 items to the client and
                 * letting it incrementally request for the previous pages,
                 * we'll cancel this flush and tweak the viewport range and
                 * flush again.
                 */
                if (assumedSize != 0 && activation.getActiveKeys().isEmpty()) {
                    int delta = viewportRange.length();
                    // Request the items from a bit behind the current range
                    // at the next call to backend, and check that the
                    // viewport range doesn't intersect the 0 point.
                    viewportRange = viewportRange.offsetBy(-delta)
                            .restrictTo(Range.withLength(0, assumedSize));
                    requestFlush(true); // to avoid recursiveness
                    return;
                }
            }
            effectiveRequested = viewportRange
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

        fireItemCountEvent(assumedSize);
    }

    /**
     * Notifies the component about item count changes.
     * <p>
     * {@link ItemCountChangeEvent} is fired if:
     * <ul>
     * <li>the passed item count differs from the item count passed on the
     * previous call of this method</li>
     * <li>Current component's filter set up to fire the event upon filtering
     * changes</li>
     * </ul>
     *
     * @param itemCount
     *            item count to send
     */
    private void fireItemCountEvent(int itemCount) {
        final boolean notify = filter == null || filter.isNotifyOnChange();

        if (lastSent != itemCount && notify) {
            final Optional<Component> component = Element.get(stateNode)
                    .getComponent();
            component.ifPresent(value -> ComponentUtil.fireEvent(value,
                    new ItemCountChangeEvent<>(value, itemCount,
                            !(isDefinedSize()
                                    || skipCountIncreaseUntilReset))));
            lastSent = itemCount;
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

    protected void doUnregister(Integer updateId) {
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
            Set<String> passivatedKeys = getPassivatedKeys(oldActive);
            if (!passivatedKeys.isEmpty()) {
                passivatedByUpdate.put(Integer.valueOf(updateId),
                        passivatedKeys);
            }
        }
    }

    protected Set<String> getPassivatedKeys(Set<String> oldActive) {
        oldActive.removeAll(activeKeyOrder);
        return oldActive;
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

            if (activeKeyOrder.isEmpty() && !effectiveRequested.isEmpty()) {
                getLogger().error(
                        "Requested data for {} but data provider did not fetch any item. "
                                + "It might be a bug in the data provider callbacks implementation ({}).",
                        effectiveRequested, getDataProvider());
            } else if (activeKeyOrder.size() < effectiveRequested.length()) {
                getLogger().error(
                        "Requested data for {} but data provider fetched only {} items. "
                                + "It might be a bug in the data provider callbacks implementation ({}).",
                        effectiveRequested, activeKeyOrder.size(),
                        getDataProvider());
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
            if (overlap.getStart() < 0) {
                // If getStart is negative there is no data and empty Activation
                // needs to be returned
                return Activation.empty();
            }
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

    private static void withMissing(Range expected, Range actual,
            Consumer<Range> action) {
        Range[] partition = expected.partitionWith(actual);

        applyIfNotEmpty(partition[0], action);
        applyIfNotEmpty(partition[2], action);
    }

    private static void applyIfNotEmpty(Range range, Consumer<Range> action) {
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
        try (Stream<T> stream = fetchFromProvider(range.getStart(),
                range.length())) {
            stream.forEach(bean -> {
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
        }
        boolean needsSizeRecheck = activeKeys.size() < range.length();
        return new Activation(activeKeys, needsSizeRecheck);
    }

    private JsonValue generateJson(T item) {
        JsonObject json = Json.createObject();
        json.put("key", getKeyMapper().key(item));
        dataGenerator.generateData(item, json);
        return json;
    }

    private void removeFilteringAndSorting() {
        Element.get(stateNode).getComponent().ifPresent(
                DataViewUtils::removeComponentFilterAndSortComparator);
    }

    private int getMaximumAllowedItems() {
        return Math.max(MAXIMUM_ALLOWED_ITEMS_LOWER_LIMIT,
                MAXIMUM_ALLOWED_PAGES * pageSize);
    }

    private UI getUI() {
        NodeOwner owner = stateNode.getOwner();
        if (owner instanceof StateTree) {
            return ((StateTree) owner).getUI();
        }
        return null;
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

    private static class FlushRequest implements Serializable {

        private NodeOwner owner;
        private boolean cancelled;

        static FlushRequest register(StateNode stateNode,
                SerializableConsumer<ExecutionContext> action) {
            FlushRequest request = new FlushRequest();
            request.owner = stateNode.getOwner();
            stateNode.runWhenAttached(ui -> {
                request.owner = stateNode.getOwner();
                if (!request.cancelled) {
                    ui.getInternals().getStateTree()
                            .beforeClientResponse(stateNode, action);
                }
            });
            return request;
        }

        boolean canExecute(StateNode stateNode) {
            return owner instanceof NullOwner || owner == stateNode.getOwner();
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DataCommunicator.class);
    }

}
