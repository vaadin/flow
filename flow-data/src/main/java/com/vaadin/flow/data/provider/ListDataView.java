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

import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

import java.util.Collection;
import java.util.Optional;

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
     * Gets the item after given item from the filtered and sorted data.
     * <p>
     * Note! Item might be present in the data set, but be filtered out
     * or be the last item so that the next item won't be available.
     *
     * @param item
     *         item to get next for
     * @return next item if available, else empty optional if item
     *         doesn't exist or not in current filtered items
     *
     * @see #getPreviousItem(Object)
     */
    Optional<T> getNextItem(T item);

    /**
     * Gets the item before given item from the filtered and sorted data.
     * <p>
     * Note! Item might be present in the data set, but be filtered out
     * or be the first item so that the previous item won't be available.
     *
     * @param item
     *         item to get previous for
     * @return previous item if available, else empty optional if item
     *         doesn't exist or not in current filtered items
     *
     * @see #getNextItem(Object)
     */
    Optional<T> getPreviousItem(T item);

    /**
     * Adds an item to the data list if it is not already present.
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
     * Adds an item after the given target item.
     * <p>
     * If the item is already present in the data provider, then it is moved.
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
     * Adds an item before the given target item.
     * <p>
     * If the item is already present in the data provider, then it is moved.
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
     * Finds an item equal to {@code item} in the non-filtered data set
     * and replaces it with {@code item}.
     * <p>
     * By default, {@code equals} method implementation of the item is used
     * for identity check. If a custom data provider is used,
     * then the {@link DataProvider#getId(Object)} method is used instead.
     * Item's custom identity can be set up with a
     * {@link DataView#setIdentifierProvider(IdentifierProvider)}.
     *
     * @param item
     *         item containing updated state
     * @return this ListDataView instance
     *
     * @throws UnsupportedOperationException
     *         if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *         if collection is not a list
     *
     * @see #setIdentifierProvider(IdentifierProvider)
     */
    V updateItem(T item);

    /**
     * Adds multiple items to the data list.
     * <p>
     * Any items that already present in the data provider are moved
     * to the end.
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
     * Any items that already present in the data provider are moved.
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
     * Any items that already present in the data provider are moved.
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
     * Sets a filter to be applied to the data. The filter replaces any filter
     * that has been set or added previously. {@code null} will clear all filters.
     * <p>
     * A filter bound to data set, not to the component. That means this filter
     * won't be retained when a new data or {@link DataProvider} is set to the
     * component. Any other component using the same {@link DataProvider} object
     * would be affected by setting a filter through data view of another
     * component.
     *
     * @param filter
     *         filter to be set, or <code>null</code> to clear any
     *         previously set filters
     * @return ListDataView instance
     *
     * @see #addFilter(SerializablePredicate)
     * @see #removeFilters()
     */
    V setFilter(SerializablePredicate<T> filter);

    /**
     * Adds a filter to be applied to all queries. The filter will be
     * used in addition to any filter that has been set or added previously.
     * <p>
     * A filter bound to data set, not to the component. That means
     * this filter and previously added filters won't be retained when
     * a new data or {@link DataProvider} is set to the component. Any other
     * component using the same {@link DataProvider} object would be affected
     * by adding a filter through data view of another component.
     *
     * @param filter
     *         the filter to add, not <code>null</code>
     * @return ListDataView instance
     *
     * @see #setFilter(SerializablePredicate)
     * @see #removeFilters()
     */
    V addFilter(SerializablePredicate<T> filter);

    /**
     * Removes all in-memory filters set or added.
     *
     * @return ListDataView instance
     *
     * @see #addFilter(SerializablePredicate)
     * @see #setFilter(SerializablePredicate)
     */
    V removeFilters();

    /**
     * Sets the comparator to use as the default sorting.
     * This overrides the sorting set by any other method that manipulates the
     * default sorting.
     * <p>
     * A comparator bound to data set, not to the component. That means
     * the default sorting won't be retained when a new data or {@link DataProvider}
     * is set to the component. Any other component using the same
     * {@link DataProvider} object would be affected by setting a sort comparator
     * through data view of another component.
     *
     * @param sortComparator
     *         a comparator to use, or <code>null</code> to clear any
     *         previously set sort order
     * @return ListDataView instance
     *
     * @see #addSortComparator(SerializableComparator)
     */
    V setSortComparator(SerializableComparator<T> sortComparator);

    /**
     * Adds a comparator to the data default sorting. If no
     * default sorting has been defined, then the provided comparator will be
     * used as the default sorting. If a default sorting has been defined, then
     * the provided comparator will be used to determine the ordering of items
     * that are considered equal by the previously defined default sorting.
     * <p>
     * A comparator added to data set, not to the component. That means
     * the default sorting won't be retained when a new data or {@link DataProvider}
     * is set to the component. Any other component using the same
     * {@link DataProvider} object would be affected by adding a sort comparator
     * through data view of another component.
     *
     * @param sortComparator
     *         a comparator to add, not <code>null</code>
     * @return ListDataView instance
     *
     * @see #setSortComparator(SerializableComparator)
     */
    V addSortComparator(SerializableComparator<T> sortComparator);

    /**
     * Removes any default sorting that has been set or added previously.
     * <p>
     * Any other component using the same {@link DataProvider} object would be affected
     * by removing default sorting through data view of another component.
     *
     * @return ListDataView instance
     *
     * @see #setSortComparator(SerializableComparator)
     * @see #addSortComparator(SerializableComparator)
     */
    V removeSorting();

    /**
     * Sets the property and direction to use as the default sorting.
     * This overrides the sorting set by any other method that
     * manipulates the default sorting of this {@link DataProvider}.
     * <p>
     * A sort order bound to data set, not to the component. That means
     * the default sorting won't be retained when a new data or
     * {@link DataProvider} is set to the component. Any other component
     * using the same {@link DataProvider} object would be affected by setting
     * a sort order through data view of another component.
     *
     * @param valueProvider
     *            the value provider that defines the property do sort by, not
     *            <code>null</code>
     * @param sortDirection
     *            the sort direction to use, not <code>null</code>
     * @param <V1>
     *            the provided value type
     *
     * @return ListDataView instance
     *
     * @see #addSortOrder(ValueProvider, SortDirection)
     */
    <V1 extends Comparable<? super V1>> V setSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection);

    /**
     * Adds a property and direction to the default sorting.
     * If no default sorting has been defined, then the provided sort
     * order will be used as the default sorting. If a default sorting has been
     * defined, then the provided sort order will be used to determine the
     * ordering of items that are considered equal by the previously defined
     * default sorting.
     * <p>
     * A sort order added to data set, not to the component. That means
     * the default sorting won't be retained when a new data or
     * {@link DataProvider} is set to the component. Any other component
     * using the same {@link DataProvider} object would be affected by adding
     * a sort order through data view of another component.
     *
     * @param valueProvider
     *            the value provider that defines the property do sort by, not
     *            <code>null</code>
     * @param sortDirection
     *            the sort direction to use, not <code>null</code>
     * @param <V1>
     *            the provided value type
     *
     * @return ListDataView instance
     *
     * @see #setSortOrder(ValueProvider, SortDirection)
     */
    <V1 extends Comparable<? super V1>> V addSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection);
}
