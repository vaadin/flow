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
