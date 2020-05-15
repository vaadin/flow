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

import com.vaadin.flow.shared.Registration;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Base class for abstract layer between {@link DataView} and {@link DataCommunicator}.
 * Allows {@link DataView} high-level API to be separated from component's specifics.
 *
 * @param <T>
 *        data type
 */
public interface DataController<T> extends Serializable {

    /**
     * Returns {@link DataProvider} instance tied with component/data communicator.
     *
     * @return data provider instance
     */
    DataProvider<T, ?> getDataProvider();

    /**
     * Add a size change listener that is fired when the data set size changes.
     * This can happen for instance when filtering the data set,
     * or if data set has been reset.
     *
     * @param listener
     *         size change listener to register
     * @return registration for removing the listener
     */
    Registration addSizeChangeListener(SizeChangeListener listener);

    /**
     * Get the full data size with filters if any set from tied data communicator or data provider.
     *
     * @return filtered data size
     */
    int getDataSize();

    /**
     * Get the full data available to the component from tied data communicator or data provider.
     * Data will use set filters and sorting.
     *
     * @return filtered and sorted data set
     */
    Stream<T> getAllItems();
}
