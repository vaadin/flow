/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;

/**
 * An interface for components that get items from the generic data provider
 * types {@link DataProvider} and {@link InMemoryDataProvider}. The methods
 * return a {@link DataView} which has the generic API for getting information
 * on the items.
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @param <V>
 *            DataView type
 * @since
 */
public interface HasDataView<T, F, V extends DataView<T>> extends Serializable {

    /**
     * Set a generic data provider for the component to use and returns the base
     * {@link DataView} that provides API to get information on the items.
     * <p>
     * This method should be used only when the data provider type is not either
     * {@link ListDataProvider} or {@link BackEndDataProvider}.
     *
     * @param dataProvider
     *            DataProvider instance to use, not <code>null</code>
     * @return DataView providing information on the data
     */
    V setItems(DataProvider<T, F> dataProvider);

    /**
     * Sets an in-memory data provider for the component to use
     * <p>
     * Note! Using a {@link ListDataProvider} instead of a
     * {@link InMemoryDataProvider} is recommended to get access to
     * {@link ListDataView} API by using
     * {@link HasListDataView#setItems(ListDataProvider)}.
     *
     * @param dataProvider
     *            InMemoryDataProvider to use, not <code>null</code>
     * @return DataView providing information on the data
     */
    V setItems(InMemoryDataProvider<T> dataProvider);

    /**
     * Get the DataView for the component.
     * <p>
     * The returned DataView only contains a minimal common API. Use of
     * {@link HasListDataView#getListDataView} or
     * {@link HasLazyDataView#getLazyDataView} should be used for more targeted
     * helper features
     *
     * @return DataView instance
     */
    V getGenericDataView();
}
