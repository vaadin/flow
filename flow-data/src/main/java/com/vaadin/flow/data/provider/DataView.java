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

import java.util.stream.Stream;

import com.vaadin.flow.shared.Registration;

/**
 * Base view interface for getting information on current
 * data set of a Component.
 *
 * @param <T>
 *         data type
 * @since
 */
public interface DataView<T> {

    /**
     * Get the full data available to the component.
     * Data will use set filters and sorting.
     *
     * @return filtered and sorted data set
     */
    Stream<T> getAllItems();

    /**
     * Get the full data size with filters is any set.
     *
     * @return filtered data size
     */
    int getDataSize();

    /**
     * Check if item is in the current data.
     * Item may be filtered out or for lazy data not in the currently loaded
     * making it un-available.
     *
     * @param item
     *         item to search for
     * @return true if item is found in the available data
     */
    boolean dataContainsItem(T item);

    /**
     * Get the item at the given row in the sorted and filetered data set.
     *
     * @param row
     *         row number
     * @return item on row
     * @throws IndexOutOfBoundsException
     *         requested row is outside of the available data set.
     */
    T getItemOnRow(int row) throws IndexOutOfBoundsException;

    /**
     * Add a size change listener that is fired when the data set size changes.
     * This can happen for instance when filtering the data set.
     *
     * @param listener
     *         size change listener to register
     * @return registration for removing the listener
     */
    Registration addSizeChangeListener(SizeChangeListener listener);
}
