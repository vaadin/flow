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

/**
 * Interface that defines methods for fetching data lazily from a backend. The
 * API will return a {@link LazyDataView}.
 *
 * @param <T>
 *            data type
 * @param <V>
 *            DataView type
 * @since
 */
public interface HasLazyDataView<T, V extends LazyDataView<T>>
        extends Serializable {
    /**
     * Supply data lazily with a callback. This sets the component to undefined
     * size and removes any existing size estimate or callback to provide size.
     * <p>
     * For using in-memory data, use
     * {@link HasListDataView#setDataSource(ListDataProvider)}
     * {@link ListDataProvider}
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query
     * @return LazyDataView instance for further configuration
     */
    default V setDataSource(
            CallbackDataProvider.FetchCallback<T, Void> fetchCallback) {
        getDataCommunicator().setDataProvider(
                DataProvider.fromCallbacks(fetchCallback, query -> -1), null);
        getDataCommunicator().setDefinedSize(false);
        return getLazyDataView();
    }

    /**
     * Supply data lazily with a callbacks. This sets the component to defined size
     * - the given count callback is queried for the data size.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query
     * @param countCallback
     *            function that return the number of items in the back end for a
     *            query
     * @return LazyDataView instance for further configuration
     */
    default V setDataSource(
            CallbackDataProvider.FetchCallback<T, Void> fetchCallback,
            CallbackDataProvider.CountCallback<T, Void> countCallback) {
        setDataSource(DataProvider.fromCallbacks(fetchCallback, countCallback));
        return getLazyDataView();
    }

    /**
     * Supply data with a {@link BackEndDataProvider} that lazy loads items from a
     * back end. This sets the component to use defined size, provided by the
     * data provider {@link BackEndDataProvider#size(Query)} method.
     *
     * @param dataProvider
     *            BackendDataProvider instance
     * @return LazyDataView instance for further configuration
     */
    default V setDataSource(BackEndDataProvider<T, Void> dataProvider) {
        getDataCommunicator().setDataProvider(dataProvider, null);
        return getLazyDataView();
    }

    /**
     * Get the LazyDataView for the component. Throws if the data is not lazy
     * and should use another data view type like {@link ListDataView}.
     *
     * @return LazyDataView instance
     * @throws IllegalStateException
     *             when lazy data view is not applicable
     */
    V getLazyDataView();

    /**
     * Gets the data communicator bound to the component. This method is meant
     * for the data views and should not be called directly.
     * 
     * @return the data communicator for the data view
     */
    DataCommunicator<T> getDataCommunicator();
}
