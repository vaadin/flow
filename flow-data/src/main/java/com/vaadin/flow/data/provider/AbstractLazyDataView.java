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

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Abstract lazy data view implementation which handles the interaction with a
 * data communicator.
 *
 * @param <T>
 *            the type of data
 */
public abstract class AbstractLazyDataView<T> extends AbstractDataView<T>
        implements LazyDataView<T> {

    private final DataCommunicator<T> dataCommunicator;

    /**
     * Creates a new instance and verifies the passed data provider is
     * compatible with this data view implementation.
     *
     * @param dataCommunicator
     *            the data communicator of the component
     * @param component
     *            the component
     */
    public AbstractLazyDataView(DataCommunicator<T> dataCommunicator,
            Component component) {
        super(dataCommunicator::getDataProvider, component);
        this.dataCommunicator = dataCommunicator;
    }

    /**
     * Returns the data communicator for the component and checks that the data
     * provider is of the correct type.
     *
     * @return the data communicator
     */
    protected DataCommunicator<T> getDataCommunicator() {
        verifyDataProviderType(dataCommunicator.getDataProvider());
        return dataCommunicator;
    }

    /**
     * Gets the item at the given index from the data available to the
     * component. Data is filtered and sorted the same way as in the component.
     * <p>
     * Calling this method with an index that is not currently active in the
     * component will cause a query to the backend, so do not call this method
     * carelessly. Use
     * {@link com.vaadin.flow.component.UI#beforeClientResponse(Component, SerializableConsumer)}
     * to access items that will be fetched later on.
     *
     * @param index
     *            the index of the item to get
     * @return item on index
     * @throws IndexOutOfBoundsException
     *             requested index is outside of the filtered and sorted data
     *             set
     */
    @Override
    public T getItem(int index) {
        return getDataCommunicator().getItem(index);
    }

    /**
     * Gets the index of the given item by the item index provider set with
     * {@link #setItemIndexProvider(ItemIndexProvider)}.
     *
     * @param item
     *            item to get index for
     * @return index of the item or null if the item is not found
     * @throws UnsupportedOperationException
     *             if the item index provider is not set with
     *             {@link #setItemIndexProvider(ItemIndexProvider)}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<Integer> getItemIndex(T item) {
        if (getItemIndexProvider() == null) {
            throw new UnsupportedOperationException(
                    "getItemIndex method in the LazyDataView requires a callback to fetch the index. Set it with setItemIndexProvider.");
        }
        return Optional.ofNullable(getItemIndexProvider().apply(item,
                getFilteredQueryForAllItems()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> getItems() {
        DataCommunicator<T> verifiedDataCommunicator = getDataCommunicator();
        return verifiedDataCommunicator.getDataProvider()
                .fetch(getQueryForAllItems());
    }

    @Override
    protected Class<?> getSupportedDataProviderType() {
        return BackEndDataProvider.class;
    }

    @Override
    public void setItemCountEstimate(int itemCountEstimate) {
        getDataCommunicator().setItemCountEstimate(itemCountEstimate);
    }

    @Override
    public int getItemCountEstimate() {
        return getDataCommunicator().getItemCountEstimate();
    }

    @Override
    public void setItemCountEstimateIncrease(int itemCountEstimateIncrease) {
        getDataCommunicator()
                .setItemCountEstimateIncrease(itemCountEstimateIncrease);
    }

    @Override
    public int getItemCountEstimateIncrease() {
        return getDataCommunicator().getItemCountEstimateIncrease();
    }

    @Override
    public void setItemCountFromDataProvider() {
        getDataCommunicator().setDefinedSize(true);
    }

    @Override
    public void setItemCountUnknown() {
        getDataCommunicator().setDefinedSize(false);
    }

    @Override
    public void setItemIndexProvider(
            ItemIndexProvider<T, ?> itemIndexProvider) {
        ComponentUtil.setData(component, ItemIndexProvider.class,
                itemIndexProvider);
    }

    /**
     * Gets the item index provider for this data view's component.
     *
     * @return the item index provider. May be null.
     */
    @SuppressWarnings("unchecked")
    protected ItemIndexProvider<T, ?> getItemIndexProvider() {
        return (ItemIndexProvider<T, ?>) ComponentUtil.getData(component,
                ItemIndexProvider.class);
    }

    private Query getQueryForAllItems() {
        DataCommunicator<T> verifiedDataCommunicator = getDataCommunicator();
        if (verifiedDataCommunicator.isDefinedSize()) {
            return verifiedDataCommunicator.buildQuery(0,
                    verifiedDataCommunicator.getItemCount());
        }
        return verifiedDataCommunicator.buildQuery(0, Integer.MAX_VALUE);
    }

    private Query getFilteredQueryForAllItems() {
        Query baseQuery = getQueryForAllItems();
        if (DataProviderWrapper.class.isAssignableFrom(
                dataCommunicator.getDataProvider().getClass())) {
            DataProviderWrapper<T, ?, ?> wrapper = (DataProviderWrapper<T, ?, ?>) dataCommunicator
                    .getDataProvider();
            return new Query(baseQuery.getOffset(), baseQuery.getLimit(),
                    baseQuery.getSortOrders(), baseQuery.getInMemorySorting(),
                    wrapper.getFilter(baseQuery));
        }
        return baseQuery;
    }

}
