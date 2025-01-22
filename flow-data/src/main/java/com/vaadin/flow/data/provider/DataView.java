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

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.ComponentEventListener;
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
     * Gets the item at the given index from the data available to the
     * component. Data is filtered and sorted the same way as in the component.
     *
     * @param index
     *            item index number
     * @return item on index
     * @throws IndexOutOfBoundsException
     *             requested index is outside of the filtered and sorted data
     *             set
     */
    T getItem(int index);

    /**
     * Gets the index of the given item from the data available to the
     * component. Data is filtered and sorted the same way as in the component.
     *
     * @param item
     *            item to get index for
     * @return index of the item or empty optional if the item is not found
     */
    Optional<Integer> getItemIndex(T item);

    /**
     * Get the full data available to the component. Data is filtered and sorted
     * the same way as in the component.
     * <p>
     * Consumers of the returned stream are responsible for closing it when all
     * the stream operations are done to ensure that any resources feeding the
     * stream are properly released. Failure to close the stream might lead to
     * resource leaks.
     * <p>
     * It is strongly recommended to use a try-with-resources block to
     * automatically close the stream after its terminal operation has been
     * executed. Below is an example of how to properly use and close the
     * stream:
     *
     * <pre>{@code
     * try (Stream<T> stream = dataView.getItems()) {
     *     stream.forEach(System.out::println); // Example terminal operation
     * }
     * }</pre>
     *
     * @return filtered and sorted data set
     */
    Stream<T> getItems();

    /**
     * Notifies the component that the item has been updated and thus should be
     * refreshed.
     * <p>
     * For this to work properly, the item must either implement
     * {@link Object#equals(Object)} and {@link Object#hashCode()} to consider
     * both the old and the new item instances to be equal, or alternatively use
     * the {@link #setIdentifierProvider(IdentifierProvider)} to set an
     * appropriate item's identifier.
     * <p>
     * This method delegates the update to
     * {@link DataProvider#refreshItem(Object)}.
     *
     * @param item
     *            item containing updated state
     *
     * @see #setIdentifierProvider(IdentifierProvider)
     */
    void refreshItem(T item);

    /**
     * Notifies the component that all the items should be refreshed.
     */
    void refreshAll();

    /**
     * Add an item count change listener that is fired when the item count
     * changes. This can happen for instance when filtering the items.
     * <p>
     * Item count change listener is bound to the component and will be retained
     * even if the data changes by setting of a new items or
     * {@link DataProvider} to component.
     * <p>
     * <em>NOTE:</em> when the component supports lazy loading (implements
     * {@link HasLazyDataView}) and a count callback has not been provided, an
     * estimate of the item count is used and increased until the actual count
     * has been reached. When the estimate is used, the event is fired with the
     * {@link ItemCountChangeEvent#isItemCountEstimated()} returning
     * {@code true}.
     *
     * @param listener
     *            item count change listener to register
     * @return registration for removing the listener
     */
    Registration addItemCountChangeListener(
            ComponentEventListener<ItemCountChangeEvent<?>> listener);

    /**
     * Sets an identifier provider, which returns an identifier for the given
     * item. The identifier is used for comparing the equality of items. Usage
     * example: {@code dataView.setIdentifiedProvider(Item::getId);}.
     *
     * @param identifierProvider
     *            function that returns the non-null identifier for a given item
     */
    void setIdentifierProvider(IdentifierProvider<T> identifierProvider);
}
