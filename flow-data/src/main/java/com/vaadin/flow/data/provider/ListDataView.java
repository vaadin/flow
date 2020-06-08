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

import java.util.Collection;

import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * DataView for a in-memory data.
 *
 * @param <T>
 *         data type
 * @param <V>
 *         ListDataView type
 * @since
 */
public interface ListDataView<T, V extends ListDataView<T, ?>>
        extends DataView<T> {
    /**
     * Check if the given item has a next item in the filtered and sorted data.
     *
     * @param item
     *         item to check if it has a next item
     * @return true if the item is present and it is not the last item
     */
    boolean hasNextItem(T item);

    /**
     * Get the item after given item from the filtered and sorted data.
     *
     * @param item
     *         item to get next for
     * @return next item if available, else null
     */
    T getNextItem(T item);

    /**
     * Check if the given item has a previous item in the filtered and sorted
     * data.
     *
     * @param item
     *         item to check if it has a previous item
     * @return true if the item is present and it is not the first item
     */
    boolean hasPreviousItem(T item);

    /**
     * Get the item before given item from the filtered and sorted data.
     *
     * @param item
     *         item to get previous for
     * @return previous item if available, else null
     */
    T getPreviousItem(T item);

    /**
     * Add an item to the data list.
     *
     * @param item
     *         item to add
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @see #addItemBefore(Object, Object)
     * @see #addItemAfter(Object, Object)
     * @see #removeItem(Object)
     */
    V addItem(T item);

    /**
     * Add an item after the given target item.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param item
     *         item to add
     * @param after
     *         item after which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *         if item doesn't exist or collection is not a list
     * @see #addItem(Object)
     * @see #addItemBefore(Object, Object)
     */
    V addItemAfter(T item, T after);

    /**
     * Add an item before the given target item.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param item
     *         item to add
     * @param before
     *         item before which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *         if item doesn't exist or collection is not a list
     * @see #addItem(Object)
     * @see #addItemAfter(Object, Object)
     */
    V addItemBefore(T item, T before);

    /**
     * Adds multiple items to the data list.
     *
     * @param items
     *         collection of item to add
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @see #removeItems(Collection)
     * @see #addItemsBefore(Collection, Object)
     * @see #addItemsAfter(Collection, Object)
     */
    V addItems(Collection<T> items);

    /**
     * Adds multiple items after the given target item.
     * The full collection is added in order after the target.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param items
     *         collection of items to add
     * @param after
     *         item after which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *         if item doesn't exist or collection is not a list
     * @see #addItems(Collection)
     * @see #addItemsBefore(Collection, Object)
     */
    V addItemsAfter(Collection<T> items, T after);

    /**
     * Adds multiple items before the given target item.
     * The full collection is added in order before the target.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param items
     *         collection of items to add
     * @param before
     *         item before which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *         if item doesn't exist or collection is not a list
     * @see #addItems(Collection)
     * @see #addItemsAfter(Collection, Object)
     */
    V addItemsBefore(Collection<T> items, T before);

    /**
     * Remove an item from the data list.
     *
     * @param item
     *         item to remove
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @see #addItem(Object)
     * @see #removeItems(Collection)
     */
    V removeItem(T item);

    /**
     * Remove multiple items from the data list.
     *
     * @param items
     *         collection of items to remove
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @see #removeItem(Object)
     * @see #removeItems(Collection)
     */
    V removeItems(Collection<T> items);

    /**
     * Adds a filter to be applied to all queries. The filter will be used in
     * addition to any filter that has been set or added previously.
     *
     * @param filter
     *         the filter to add, not <code>null</code>
     * @return ListDataView instance
     */
    V addFilter(SerializablePredicate<T> filter);

    /**
     * Remove all in-memory filters set or added.
     *
     * @return ListDataView instance
     */
    V clearFilters();

    /**
     * Set a filter to be applied to the data. Given filter replaces any
     * previous filter. Setting {@code null} clears filtering.
     *
     * @param filter
     *         filter to add for the data
     * @return ListDataView instance
     */
    V withFilter(SerializablePredicate<T> filter);

    /**
     * Sets the comparator to use as the default sorting for data.
     * This overrides the sorting set by any other method that manipulates the
     * default sorting of the data.
     *
     * @param sortComparator
     *         a comparator to use, or <code>null</code> to clear any
     *         previously set sort order
     * @return ListDataView instance
     */
    V withSortComparator(SerializableComparator<T> sortComparator);
}
