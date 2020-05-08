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

import java.util.Optional;

/**
 * DataView for lazy loaded data.
 *
 * @param <T>
 *         data type
 * @since
 */
public interface LazyDataView<T> extends DataView<T> {

    /**
     * Get the item after the given item if available on the server. Server
     * only knows the latest set requested by the client and will be filtered
     * and sorted accordingly.
     *
     * @param item
     *         item to get next from
     * @return optional containing next item if available
     */
    Optional<T> getNextItem(T item);

    /**
     * Get the item before the given item if available on the server. Server
     * only knows the latest set requested by the client and will be filtered
     * and sorted accordingly.
     *
     * @param item
     *         item to get previous from
     * @return optional containing previous item if available
     */
    Optional<T> getPreviousItem(T item);

    // API related supporting undefined size - it makes no sense to have these for in-memory

    /**
     * Add count callback for lazy data with undefined size.
     *
     * @param callback
     *         count callback to use
     */
    void withCountCallback(
            CallbackDataProvider.CountCallback<T, Void> callback);

    /**
     * Set an initial estimate for the item count.
     *
     * @param initialCountEstimate
     *         initial count estimate
     */
    void withInitialCountEstimate(int initialCountEstimate);

    /**
     * Add a count callback for estimating the remaining size of the data.
     *
     * @param callback
     *         count estimation to use
     */
    void withCountEstimateCallback(
            CallbackDataProvider.CountCallback<T, Void> callback);
}
