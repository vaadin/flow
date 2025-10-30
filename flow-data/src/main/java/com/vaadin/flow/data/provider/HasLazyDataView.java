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
import java.util.Collection;

/**
 * Interface that defines methods for fetching items lazily from a backend. The
 * API will return a {@link LazyDataView}.
 *
 * @param <T>
 *            item type
 * @param <F>
 *            filter type
 * @param <V>
 *            DataView type
 */
public interface HasLazyDataView<T, F, V extends LazyDataView<T>>
        extends Serializable {

    /**
     * Supply items lazily with a callback from a backend. The component will
     * automatically fetch more items and adjust its size until the backend runs
     * out of items. Usage example without component provided filter:
     * <p>
     * {@code component.setItems(query -> orderService.getOrders(query.getOffset(), query.getLimit());}
     * <p>
     * If the component supports filtering, it can be fetched with
     * query.getFilter().
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getLazyDataView()}. For using in-memory
     * data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the backend based
     *            on the offset, limit and an optional filter provided by the
     *            query object
     * @return LazyDataView instance for further configuration
     */
    default V setItems(CallbackDataProvider.FetchCallback<T, F> fetchCallback) {
        setItems(DataProvider.fromFilteringCallbacks(fetchCallback, query -> {
            throw new IllegalStateException(
                    "Trying to use exact size with a lazy loading component"
                            + " without either providing a count callback for the"
                            + " component to fetch the count of the items or a data"
                            + " provider that implements the size query. Provide the "
                            + "callback for fetching item count with%n"
                            + "component.getLazyDataView().setItemCountCallback(CallbackDataProvider.CountCallback);"
                            + "%nor switch to undefined size with%n"
                            + "component.getLazyDataView().setItemCountUnknown();");
        }));
        V lazyDataView = getLazyDataView();
        lazyDataView.setItemCountUnknown();
        return lazyDataView;
    }

    /**
     * Supply items lazily with callbacks: the first one fetches the items based
     * on offset, limit and an optional filter, the second provides the exact
     * count of items in the backend. Use this in case getting the count is
     * cheap and the user benefits from the component showing immediately the
     * exact size. Usage example without component provided filter:
     * <p>
     * {@code component.setItems(
     *                    query -> orderService.getOrders(query.getOffset, query.getLimit()),
     *                    query -> orderService.getSize());}
     * <p>
     * If the component supports filtering, it can be fetched with
     * query.getFilter().
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getLazyDataView()}. For using in-memory
     * data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query
     * @param countCallback
     *            function that return the number of items in the back end for a
     *            query
     * @return LazyDataView instance for further configuration
     */
    default V setItems(CallbackDataProvider.FetchCallback<T, F> fetchCallback,
            CallbackDataProvider.CountCallback<T, F> countCallback) {
        setItems(DataProvider.fromFilteringCallbacks(fetchCallback,
                countCallback));
        return getLazyDataView();
    }

    /**
     * Supply items with a {@link BackEndDataProvider} that lazy loads items
     * from a backend. Note that component will query the data provider for the
     * item count. In case that is not desired for performance reasons, use
     * {@link #setItems(CallbackDataProvider.FetchCallback)} instead.
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getLazyDataView()}. For using in-memory
     * data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     *
     * @param dataProvider
     *            BackEndDataProvider instance
     * @return LazyDataView instance for further configuration
     */
    V setItems(BackEndDataProvider<T, F> dataProvider);

    /**
     * Get the LazyDataView for the component that allows access to the items in
     * the component. Throws an exception if the items are not provided lazily
     * and another data view type should be used, like {@link ListDataView}.
     *
     * @return LazyDataView instance
     * @throws IllegalStateException
     *             when lazy data view is not applicable
     */
    V getLazyDataView();
}
