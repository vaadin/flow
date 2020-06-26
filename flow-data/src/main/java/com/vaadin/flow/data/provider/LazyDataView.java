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
 * DataView for lazy loaded data.
 *
 * @param <T>
 *            data type
 * @since
 */
public interface LazyDataView<T> extends DataView<T> {

    /**
     * Sets a callback that the component uses to get the exact rows count
     * (items) in the data source. Use this when it is cheap to get the exact
     * size of the data source and it is desired that the user sees the "full
     * scrollbar size".
     * <p>
     * The given callback will be queried for the count instead of the data
     * provider {@link DataProvider#size(Query)} method when the component has a
     * distinct data provider set with
     * {@link HasLazyDataView#setDataSource(BackEndDataProvider)}.
     *
     * @param callback
     *            the callback to use for determining row count in the data
     *            source, not {@code null}
     * @see #setRowCountFromDataProvider()
     * @see #setRowCountUnknown()
     */
    void setRowCountCallback(
            CallbackDataProvider.CountCallback<T, Void> callback);

    /**
     * Switches the component to get the exact row count from the data
     * provider's {@link DataProvider#size(Query)}. Use this when it is cheap to
     * get the exact size of the data source and it is desired that the user
     * sees the "full scrollbar size".
     * <p>
     * Calling this method will clear any previously set count callback with the
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)} method.
     */
    void setRowCountFromDataProvider();

    /**
     * Sets the estimated row count for the component. The component will
     * automatically fetch more items once the estimate is reached or adjust the
     * count if the data source runs out of items before the end. Use this when
     * the data source will have a lot more items than shown by default and it
     * should be shown to the user.
     * <p>
     * The given estimate is discarded if it is less than the currently shown
     * range or if the actual number of rows has been determined. The estimate
     * shouldn't be less than two pages (see {@code setPageSize(int)} in the
     * component) or it causes unnecessary backend requests.
     * <p>
     * Calling this method will clear any previously set count callback
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)}.
     *
     * @param rowCountEstimate
     *            estimated row count of the data source
     * @see #setRowCountUnknown()
     * @see #setRowCountEstimateIncrease(int)
     */
    void setRowCountEstimate(int rowCountEstimate);

    /**
     * Gets the row count estimate. The default value depends on the component.
     *
     * @return row count estimate
     * @see #setRowCountEstimate(int)
     */
    int getRowCountEstimate();

    /**
     * Sets how much the row count estimate is increased once the previous row
     * count estimate has been reached. Use this when the user should be able to
     * scroll down faster. The default value depends on the component.
     * <p>
     * As an example, with an estimate of {@code 1000} and an increase of
     * {@code 500}, when scrolling down the row count will be:
     * {@code 1000, 1500, 2000, 2500...} until the data source runs out of
     * items.
     * <p>
     * <em>NOTE:</em> the given increase should not be less than the
     * {@code setPageSize(int)} set in the component, or there will be
     * unnecessary backend requests.
     *
     * @param rowCountEstimateIncrease
     *            how much the row count estimate is increased
     * @see #setRowCountEstimate(int)
     */
    void setRowCountEstimateIncrease(int rowCountEstimateIncrease);

    /**
     * Gets the row count estimate increase - how much the row count estimate is
     * increased once the previous row count estimate has been reached. The
     * default value depends on the component.
     * 
     * @return the row count estimate increase
     * @see #setRowCountEstimateIncrease(int)
     */
    int getRowCountEstimateIncrease();

    /**
     * Switches the component to automatically extend the number of rows as the
     * previous end is almost reached. The component stops increasing the number
     * of rows once the data source has run out of items. Not getting the exact
     * size of the data source upfront can improve performance with large sets
     * of data.
     * <p>
     * The default initial row count and how much the row count is increased
     * depends on the component. These values can be customized with
     * {@link #setRowCountEstimate(int)} and
     * {@link #setRowCountEstimateIncrease(int)} when the data source has a lot
     * of items and faster scrolling down is desired.
     * <p>
     * Calling this method will clear any previously set count callback
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)}.
     */
    void setRowCountUnknown();
}
