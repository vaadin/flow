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
     * (items) in the data source.
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
     * provider's {@link DataProvider#size(Query)}.
     * <p>
     * Calling this method will clear any previously set count callback with the
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)} method.
     */
    void setRowCountFromDataProvider();

    /**
     * Sets the estimated row count for the component. The component will
     * automatically fetch more items once the estimate is reached or adjust the
     * count if the data source runs out of items before the end.
     * <p>
     * The given estimate is discarded if it is less than the currently shown
     * range or if the actual number of rows has been determined. The estimate
     * shouldn't be less than two pages (see {@code setPageSize(int)} in the
     * component) or it might cause extra requests after a reset.
     * <p>
     * Calling this method will clear any previously set count callback
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)}.
     *
     * @param rowCountEstimate
     *            estimated row count of the data source
     * @see #setRowCountUnknown()
     * @see #setRowCountEstimateStep(int)
     */
    void setRowCountEstimate(int rowCountEstimate);

    /**
     * Gets the row count estimate. The default value depens on the component.
     *
     * @return row count estimate
     * @see #setRowCountEstimate(int)
     */
    int getRowCountEstimate();

    /**
     * Sets how much the row count estimate is increased once the previous row
     * count estimate has been reached. The default value depends on the
     * component.
     * <p>
     * <em>NOTE:</em> the given step should not be less than the
     * {@code setPageSize(int)} set in the component, or there will
     *
     * @param rowCountEstimateStep
     *            row count estimate step
     * @see #setRowCountEstimate(int)
     */
    void setRowCountEstimateStep(int rowCountEstimateStep);

    /**
     * Gets the row count estimate step - how much the row count estimate is
     * increased once the previous row count estimate has been reached. The
     * default value depends on the component.
     * 
     * @return the row count estimate step
     * @see #setRowCountEstimateStep(int)
     */
    int getRowCountEstimateStep();

    /**
     * Switches the component to automatically extend the number of rows as the
     * previous end is almost reached. The component stop increasing the number
     * of rows once the data source has run out of items.
     * <p>
     * The default initial row count and how much the row count is increased
     * depends on the component. These values can be customized with
     * {@link #setRowCountEstimate(int)} and
     * {@link #setRowCountEstimateStep(int)}.
     * <p>
     * Calling this method will clear any previously set count callback
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)}.
     */
    void setRowCountUnknown();
}
