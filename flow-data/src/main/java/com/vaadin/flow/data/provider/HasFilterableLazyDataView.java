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
import java.util.Collection;

import com.vaadin.flow.function.SerializableFunction;

/**
 * Defines the methods for fetching items lazily from a backend taking into
 * account the configured filter. The API will return a
 * {@link FilterableLazyDataView}.
 * <p>
 * <em>Note:</em> If you don't need a configured filter in your data access
 * layer, please use {@link HasLazyDataView} instead.
 * 
 * @param <T>
 *            item type
 * @param <F>
 *            filter type
 * @param <V>
 *            DataView type
 */
public interface HasFilterableLazyDataView<T, F, V extends FilterableLazyDataView<T, F>>
        extends Serializable {

    /**
     * Supply items lazily with a callback from a backend, taking into account
     * the configured filter. The component will automatically fetch more items
     * and adjust its size until the backend runs out of items. Usage example:
     * <p>
     * {@code component.setItemsWithFilter(query -> orderService
     *      .getOrders(query.getOffset(), query.getLimit(), query.getFilter());}
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getFilterableLazyDataView()}. For using
     * in-memory data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     * <p>
     * <em>Note:</em> this method is mutually exclusive with the other
     * callbacks/data provider setting methods of {@code
     * HasFilterableLazyDataView}, i.e. calling each of them means that the
     * previously set callbacks/data providers will be replaced by a new ones.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the backend based
     *            on the offset, limit and filter provided by the query object,
     *            not <code>null</code>
     * @return FilterableLazyDataView instance for further configuration
     *
     * @see #setItemsWithConvertedFilter(CallbackDataProvider.FetchCallback, SerializableFunction)
     */
    default V setItemsWithFilter(
            CallbackDataProvider.FetchCallback<T, F> fetchCallback) {
        return setItemsWithConvertedFilter(fetchCallback,
                SerializableFunction.identity());
    }

    /**
     * Supply items lazily with callbacks: the first one fetches the items based
     * on offset and limit, the second provides the exact count of items in the
     * backend. Both callbacks use a filter object when fetching the items or
     * getting the size. Use this in case getting the count is cheap and the
     * user benefits from the component showing immediately the exact size.
     * Usage example:
     * <p>
     * {@code component.setItemsWithFilter(
     *                    query -> orderService.getOrders(query.getOffset, 
     *                                     query.getLimit(), query.getFilter()),
     *                    query -> orderService.getSize(query.getFilter()));}
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getFilterableLazyDataView()}. For using
     * in-memory data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     * <p>
     * <em>Note:</em> this method is mutually exclusive with the other
     * callbacks/data provider setting methods of {@code
     * HasFilterableLazyDataView}, i.e. calling each of them means that the
     * previously set callbacks/data providers will be replaced by a new ones.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query, not <code>null</code>
     * @param countCallback
     *            function that return the number of items in the back end for a
     *            query, not <code>null</code>
     * @return FilterableLazyDataView instance for further configuration
     *
     * @see #setItemsWithFilter(BackEndDataProvider)
     */
    default V setItemsWithFilter(
            CallbackDataProvider.FetchCallback<T, F> fetchCallback,
            CallbackDataProvider.CountCallback<T, F> countCallback) {
        return setItemsWithFilter(DataProvider
                .fromFilteringCallbacks(fetchCallback, countCallback));
    }

    /**
     * Supply items lazily with a callback from a backend, taking into account a
     * filter generated by the given converter. The component will automatically
     * fetch more items and adjust its size until the backend runs out of items.
     * Usage example:
     * <p>
     * {@code component.setItemsWithFilter(query -> orderService
     *      .getOrders(query.getOffset(), query.getLimit(), query.getFilter());}
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getFilterableLazyDataView()}. For using
     * in-memory data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     * <p>
     * <em>Note:</em> this method is mutually exclusive with the other
     * callbacks/data provider setting methods of {@code
     * HasFilterableLazyDataView}, i.e. calling each of them means that the
     * previously set callbacks/data providers will be replaced by a new ones.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query, not <code>null</code>
     * @param filterConverter
     *            a function that converts the component's configured filter to
     *            the filter values expected by the fetch callback, not
     *            <code>null</code>
     * @param <Q>
     *            fetch callback filter type
     * @return FilterableLazyDataView instance for further configuration
     *
     * @see #setItemsWithConvertedFilter(CallbackDataProvider.FetchCallback, CallbackDataProvider.CountCallback, SerializableFunction)
     */
    default <Q> V setItemsWithConvertedFilter(
            CallbackDataProvider.FetchCallback<T, Q> fetchCallback,
            SerializableFunction<F, Q> filterConverter) {
        V filterableLazyDataView = setItemsWithConvertedFilter(fetchCallback,
                query -> {
                    throw new IllegalStateException(
                            "Trying to use exact size with a lazy loading component"
                                    + " without either providing a count callback for the"
                                    + " component to fetch the count of the items or a data"
                                    + " provider that implements the size query. Provide the "
                                    + "callback for fetching item count with%n"
                                    + "component.getFilterableLazyDataView().withDefinedSize(CallbackDataProvider.CountCallback);"
                                    + "%nor switch to undefined size with%n"
                                    + "component.getFilterableLazyDataView().withUndefinedSize();");
                }, filterConverter);
        filterableLazyDataView.setItemCountUnknown();
        return filterableLazyDataView;
    }

    /**
     * Supply items with a {@link BackEndDataProvider} that lazy loads items
     * from a backend. Note that component will query the data provider for the
     * item count. In case that is not desired for performance reasons, use
     * {@link #setItemsWithFilter(CallbackDataProvider.FetchCallback)} or
     * {@link #setItemsWithConvertedFilter(CallbackDataProvider.FetchCallback, SerializableFunction)}
     * instead.
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getFilterableLazyDataView()}. For using
     * in-memory data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     * <p>
     * <em>Note:</em> this method is mutually exclusive with the other
     * callbacks/data provider setting methods of {@code
     * HasFilterableLazyDataView}, i.e. calling each of them means that the
     * previously set callbacks/data providers will be replaced by a new ones.
     *
     * @param dataProvider
     *            BackEndDataProvider instance, not <code>null</code>
     * @return FilterableLazyDataView instance for further configuration
     *
     * @see #setItemsWithFilter(CallbackDataProvider.FetchCallback, CallbackDataProvider.CountCallback)
     */
    V setItemsWithFilter(BackEndDataProvider<T, F> dataProvider);

    /**
     * Supply items lazily with callbacks: the first one fetches the items based
     * on offset and limit, the second provides the exact count of items in the
     * backend. Both callbacks use a filter object when fetching an items or
     * getting the size. This filter is generated by the given filter converter.
     * Use this in case getting the count is cheap and the user benefits from
     * the component showing immediately the exact size. Usage example:
     * <p>
     * {@code component.setItemsWithFilter(
     *                    query -> orderService.getOrders(query.getOffset,
     *                                     query.getLimit(), query.getFilter()),
     *                    query -> orderService.getSize(query.getFilter()));}
     * <p>
     * The returned data view object can be used for further configuration, or
     * later on fetched with {@link #getFilterableLazyDataView()}. For using
     * in-memory data, like {@link java.util.Collection}, use
     * {@link HasListDataView#setItems(Collection)} instead.
     * <p>
     * <em>Note:</em> this method is mutually exclusive with the other
     * callbacks/data provider setting methods of {@code
     * HasFilterableLazyDataView}, i.e. calling each of them means that the
     * previously set callbacks/data providers will be replaced by a new ones.
     *
     * @param fetchCallback
     *            function that returns a stream of items from the back end for
     *            a query, not <code>null</code>
     * @param countCallback
     *            function that return the number of items in the back end for a
     *            query, not <code>null</code>
     * @param filterConverter
     *            a function that converts the component's configured filter to
     *            the filter values expected by the fetch callback, not
     *            <code>null</code>
     * @param <Q>
     *            fetch/count callback filter type
     * @return FilterableLazyDataView instance for further configuration
     *
     * @see #setItemsWithConvertedFilter(CallbackDataProvider.FetchCallback, SerializableFunction)
     * @see #setItemsWithFilter(CallbackDataProvider.FetchCallback)
     */
    <Q> V setItemsWithConvertedFilter(
            CallbackDataProvider.FetchCallback<T, Q> fetchCallback,
            CallbackDataProvider.CountCallback<T, Q> countCallback,
            SerializableFunction<F, Q> filterConverter);

    /**
     * Gets the FilterableLazyDataView for the component that allows access to
     * the items in the component, including items filtering API. Throws an
     * exception if the items are not provided lazily and another data view type
     * should be used, like {@link ListDataView}.
     *
     * @return FilterableLazyDataView instance
     * @throws IllegalStateException
     *             when lazy data view is not applicable
     */
    V getFilterableLazyDataView();
}
