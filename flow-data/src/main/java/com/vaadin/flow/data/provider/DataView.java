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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Base view interface for getting information on current data set of a
 * Component.
 *
 * @param <T>
 *            data type
 * @since
 */
public interface DataView<T> extends Serializable {

    /**
     * Get the full data available to the component. Data will use set filters
     * and sorting.
     * <p>
     * <em>NOTE:</em> calling this method might cause a backend query
     * that fetches all items from the backend when using a lazy data
     * source!
     *
     * @return filtered and sorted data set
     */
    Stream<T> getItems();

    /**
     * Get the full data size with filters if any set. As the size might change
     * at any point, it is recommended to add a listener with the
     * {@link #addSizeChangeListener(ComponentEventListener)} method instead to
     * get notified when the data size has changed.
     *
     * @return filtered data size
     */
    int getSize();

    /**
     * Check if item is in the current data. Item may be filtered out or for
     * lazy data not in the currently loaded making it un-available.
     * <p>
     * <em>NOTE:</em> when the component is created and not yet added, the item
     * might not yet be loaded, but will be loaded during the "before client
     * response"-phase. To check if the item has been added at that point, after
     * setting the data source to the component you need to use
     * {@link com.vaadin.flow.component.UI#beforeClientResponse(Component, SerializableConsumer)}.
     *
     * @param item
     *            item to search for
     * @return {@code true} if item is found in the available data
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
     *            size change listener to register
     * @return registration for removing the listener
     */
    Registration addSizeChangeListener(
            ComponentEventListener<SizeChangeEvent<?>> listener);
}
