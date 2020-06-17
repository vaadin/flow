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
     * Sets the component to use defined size with the given callback that
     * provides the exact size for the dataset.
     * <p>
     * When the component has a distinct data provider set with
     * {@link com.vaadin.flow.data.binder.HasDataProvider#setDataProvider(DataProvider)},
     * the given callback will be queried for getting the dataset size instead
     * of the data provider {@link DataProvider#size(Query)} method.
     * <p>
     * Calling this method will clear any previously set initial size estimate
     * {@link #withUndefinedSize(int)} and size estimate callback
     * {@link #withUndefinedSize(SizeEstimateCallback)}.
     *
     * @param callback
     *            the callback to use for determining dataset size, not
     *            {@code null}
     * @see #withDefinedSize()
     */
    void withDefinedSize(CallbackDataProvider.CountCallback<T, Void> callback);

    /**
     * Sets the component to use undefined size with the given initial size
     * estimate of the dataset. The estimated size is used after reset occurs or
     * filter changes. <em>The initial size estimate should not be less than the currently
     * requested range, or it will get discarded until a reset occurs.</em>.
     * <p>
     * Calling this method will clear any previously set size estimate callback
     * {@link #withUndefinedSize(SizeEstimateCallback)} or defined size callback
     * {@link #withDefinedSize(CallbackDataProvider.CountCallback)}.
     *
     * @param initialSizeEstimate
     *            initial size estimate for the dataset
     */
    void withUndefinedSize(int initialSizeEstimate);

    /**
     * Sets the component to use undefined size with the given callback for
     * estimating the size of the dataset. Calling this method will clear any
     * previously set size callback
     * {@link #withDefinedSize(CallbackDataProvider.CountCallback)} or initial
     * size estimate {@link #withUndefinedSize(int)}.
     * <p>
     * This callback is triggered:
     * <ol>
     * <li>initially after setting</li>
     * <li>after reset like when the filter has changed</li>
     * <li>when the last page of the previous size is being fetched</li>
     * </ol>
     * <p>
     * Once the size is known, because the backend has "run out of items", the
     * callback is not triggered until a reset occurs or filter changes.
     * <p>
     * <em>NOTE: The given callback can only return a size estimate that is less
     * than the {@link SizeEstimateQuery#getRequestedRangeEnd()} when there has
     * been a reset (see {@link SizeEstimateQuery#isReset()},as that would be
     * the same as using defined size.</em> An {@link IllegalStateException} is
     * thrown instead. For using defined size, use
     * {@link #withDefinedSize(CallbackDataProvider.CountCallback)} instead.
     *
     * @param callback
     *            a callback that provides the estimated size of the data
     */
    void withUndefinedSize(SizeEstimateCallback<T, Void> callback);

    /**
     * Switches the component to defined size from undefined size.
     * <p>
     * With defined size, the data provider in the component needs to have
     * implemented the {@link DataProvider#size(Query)} method. Another
     * alternative is to provide a size callback with the
     * {@link #withDefinedSize(CallbackDataProvider.CountCallback)}.
     */
    void withDefinedSize();

    /**
     * Returns whether the component is using defined or undefined size.
     * 
     * @return {@code true} for defined size, {@code false} for undefined size
     * @see #withDefinedSize()
     * @see #withUndefinedSize()
     */
    boolean isDefinedSize();

    /**
     * Switches the component to undefined size from defined size or from
     * previous estimate #withInitialCountEstimate or estimate callback
     * #withCountEstimateCallback.
     * <p>
     * The default undefined size depends on the component implementation. For
     * controlling the undefined size, use {@link #withUndefinedSize(int)} or
     * {@link #withUndefinedSize(SizeEstimateCallback)}. Calling this method
     * will clear any previously set initial size estimate or size.
     */
    void withUndefinedSize();
}
