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

/**
 * DataView for lazy loaded filtered data.
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @since
 */
public interface FilterableLazyDataView<T, F> extends LazyDataView<T> {

    /**
     * Sets the configured filter to the component. This filter will be
     * available in data fetch and count callbacks with
     * {@link Query#getFilter()}.
     *
     * @param filter
     *            the configured filter to set, or <code>null</code> to clear
     *            any previously set filter
     *
     * @see #removeFilter()
     */
    void setFilter(F filter);

    /**
     * Removes a filter from component.
     *
     * @see #setFilter(Object)
     */
    default void removeFilter() {
        setFilter(null);
    }

    /**
     * Sets the filter combiner function which will be used to merge the
     * previous configured filter and a new filter set with
     * {@link #setFilter(Object)}.
     * 
     * @param filterCombiner
     *            function for combining a previous component's configured
     *            filter and a new configured filter, not <code>null</code>
     *
     * @see #setFilter(Object)
     */
    void setFilterCombiner(FilterCombiner<F> filterCombiner);

    /**
     * Sets a callback that the component uses to get the exact item count in
     * the backend taking into account the given filter. Use this when it is
     * cheap to get the exact item count and it is desired that the user sees
     * the "full scrollbar size".
     * <p>
     * The given callback will be queried for the count instead of the data
     * provider {@link DataProvider#size(Query)} method when the component has a
     * distinct data provider set with
     * {@link HasFilterableLazyDataView#setItemsWithFilter(BackEndDataProvider)}.
     *
     * @param callback
     *            the callback to use for determining item count in the backend,
     *            not {@code null}
     * @see #setItemCountFromDataProvider()
     * @see #setItemCountUnknown()
     */
    void setItemCountCallbackWithFilter(
            CallbackDataProvider.CountCallback<T, F> callback);
}
