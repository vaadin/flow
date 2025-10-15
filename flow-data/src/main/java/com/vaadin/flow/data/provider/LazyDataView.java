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

/**
 * DataView for lazy loaded data.
 *
 * @param <T>
 *            data type
 */
public interface LazyDataView<T> extends DataView<T> {

    /**
     * Switches the component to get the exact item count from the data
     * provider's {@link DataProvider#size(Query)}. Use this when it is cheap to
     * get the exact item count and it is desired that the user sees the "full
     * scrollbar size".
     */
    void setItemCountFromDataProvider();

    /**
     * Sets the estimated item count for the component. The component will
     * automatically fetch more items once the estimate is reached or adjust the
     * count if the backend runs out of items before the end. Use this when the
     * backend will have a lot more items than shown by default and it should be
     * shown to the user.
     * <p>
     * The given estimate is discarded if it is less than the currently shown
     * range or if the actual number of items has been determined. The estimate
     * shouldn't be less than two pages (see {@code setPageSize(int)} in the
     * component) or it causes unnecessary backend requests.
     *
     * @param itemCountEstimate
     *            estimated item count of the backend
     * @see #setItemCountUnknown()
     * @see #setItemCountEstimateIncrease(int)
     */
    void setItemCountEstimate(int itemCountEstimate);

    /**
     * Gets the item count estimate. The default value depends on the component.
     *
     * @return item count estimate
     * @see #setItemCountEstimate(int)
     */
    int getItemCountEstimate();

    /**
     * Sets how much the item count estimate is increased once the previous item
     * count estimate has been reached. Use this when the user should be able to
     * scroll down faster. The default value depends on the component.
     * <p>
     * As an example, with an estimate of {@code 1000} and an increase of
     * {@code 500}, when scrolling down the item count will be:
     * {@code 1000, 1500, 2000, 2500...} until the backend runs out of items.
     * <p>
     * <em>NOTE:</em> the given increase should not be less than the
     * {@code setPageSize(int)} set in the component, or there will be
     * unnecessary backend requests.
     *
     * @param itemCountEstimateIncrease
     *            how much the item count estimate is increased
     * @see #setItemCountEstimate(int)
     */
    void setItemCountEstimateIncrease(int itemCountEstimateIncrease);

    /**
     * Gets the item count estimate increase - how much the item count estimate
     * is increased once the previous item count estimate has been reached. The
     * default value depends on the component.
     *
     * @return the item count estimate increase
     * @see #setItemCountEstimateIncrease(int)
     */
    int getItemCountEstimateIncrease();

    /**
     * Switches the component to automatically extend the number of items as the
     * previous end is almost reached. The component stops increasing the number
     * of items once the backend has run out of items. Not getting the exact
     * size of the backend upfront can improve performance with large sets of
     * data.
     * <p>
     * The default initial item count and how much the item count is increased
     * depends on the component. These values can be customized with
     * {@link #setItemCountEstimate(int)} and
     * {@link #setItemCountEstimateIncrease(int)} when the backend has a lot of
     * items and faster scrolling down is desired.
     */
    void setItemCountUnknown();

    /**
     * Sets the item index provider for this lazy data view. The provider is
     * used to fetch the index of an item.
     * {@link ItemIndexProvider#apply(Object, Query)} is called when
     * {@link #getItemIndex(Object)} is called. It gives an item and a
     * {@link Query} as parameters. Query is prepared for fetching all items
     * including filter and sorting.
     *
     * @param itemIndexProvider
     *            the item index provider to use
     */
    void setItemIndexProvider(ItemIndexProvider<T, ?> itemIndexProvider);
}
