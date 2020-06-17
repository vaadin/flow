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
import java.util.stream.Stream;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

/**
 * Base view interface for getting information on current
 * data set of a Component.
 *
 * @param <T>
 *         data type
 * @since
 */
public interface DataView<T> extends Serializable {

    /**
     * Get the full data available to the component.
     * Data will use set filters and sorting.
     *
     * @return filtered and sorted data set
     */
    Stream<T> getItems();

    /**
     * Get the full data size with filters if any set.
     *
     * @return filtered data size
     */
    int getSize();

    /**
     * Check if item is in the current data.
     * Item may be filtered out or for lazy data not in the currently loaded
     * making it un-available.
     * <p>
     * By default, equality between the items is determined by the identifiers
     * provided by {@link DataProvider#getId(Object)}. Identity provider can
     * be changed with a {@link DataView#setIdentityProvider(ValueProvider)}.
     *
     * @param item
     *         item to search for
     * @return true if item is found in the available data
     *
     * @see #setIdentityProvider(ValueProvider)
     */
    boolean contains(T item);

    /**
     * Add a size change listener that is fired when the data set size changes.
     * This can happen for instance when filtering the data set.
     * <p>
     * Size change listener is bound to the component and will be retained even
     * if the data changes by setting of a new items or {@link DataProvider} to
     * component.
     *
     * @param listener
     *         size change listener to register
     * @return registration for removing the listener
     */
    Registration addSizeChangeListener(
            ComponentEventListener<SizeChangeEvent<?>> listener);

    /**
     * Sets identity provider to be used for getting item identifier and
     * compare the items using that identifier.
     *
     * @param identityProvider
     *           function that returns the non-null identifier for a given item
     */
    void setIdentityProvider(ValueProvider<T, ?> identityProvider);
}
