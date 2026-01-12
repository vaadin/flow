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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

/**
 * DataView for a in-memory list data that provides information on the data and
 * allows operations on it. Mutation operations can be used only if the backing
 * {@link List} is mutable.
 *
 * @param <T>
 *            data type
 * @param <V>
 *            ListDataView type
 */
public interface ListDataView<T, V extends ListDataView<T, ?>>
        extends DataView<T> {

    /**
     * Check if item is present in the currently filtered data set.
     * <p>
     * By default, {@code equals} method implementation of the item is used for
     * identity check. If a custom data provider is used, then the
     * {@link DataProvider#getId(Object)} method is used instead. Item's custom
     * identity can be set up with a
     * {@link DataView#setIdentifierProvider(IdentifierProvider)}.
     *
     * @param item
     *            item to search for
     * @return {@code true} if item is found in filtered data set
     *
     * @see #setIdentifierProvider(IdentifierProvider)
     */
    boolean contains(T item);

    /**
     * Get the full item count with filters if any set. As the item count might
     * change at any point, it is recommended to add a listener with the
     * {@link #addItemCountChangeListener(ComponentEventListener)} method
     * instead to get notified when the item count has changed.
     *
     * @return filtered item count
     * @see #addItemCountChangeListener(ComponentEventListener)
     */
    int getItemCount();

    /**
     * Gets the item after given item from the filtered and sorted data.
     * <p>
     * Note! Item might be present in the data set, but be filtered out or be
     * the last item so that the next item won't be available.
     *
     * @param item
     *            item to get next for
     * @return next item if available, else empty optional if item doesn't exist
     *         or not in current filtered items
     *
     * @see #getPreviousItem(Object)
     */
    Optional<T> getNextItem(T item);

    /**
     * Gets the item before given item from the filtered and sorted data.
     * <p>
     * Note! Item might be present in the data set, but be filtered out or be
     * the first item so that the previous item won't be available.
     *
     * @param item
     *            item to get previous for
     * @return previous item if available, else empty optional if item doesn't
     *         exist or not in current filtered items
     *
     * @see #getNextItem(Object)
     */
    Optional<T> getPreviousItem(T item);

    /**
     * Adds an item to the data list if it is not already present.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * Refreshes all items of the component after adding the item, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param item
     *            item to add
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @see #addItemBefore(Object, Object)
     * @see #addItemAfter(Object, Object)
     * @see #removeItem(Object)
     */
    V addItem(T item);

    /**
     * Adds an item after the given target item.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * If the item is already present in the data provider, then it is moved.
     * <p>
     * Refreshes all items of the component after adding the item, i.e. runs
     * {@link DataView#refreshAll()}.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param item
     *            item to add
     * @param after
     *            item after which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *             if item doesn't exist or collection is not a list
     * @see #addItem(Object)
     * @see #addItemBefore(Object, Object)
     */
    V addItemAfter(T item, T after);

    /**
     * Adds an item before the given target item.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * If the item is already present in the data provider, then it is moved.
     * <p>
     * Refreshes all items of the component after adding the item, i.e. runs
     * {@link DataView#refreshAll()}.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param item
     *            item to add
     * @param before
     *            item before which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *             if item doesn't exist or collection is not a list
     * @see #addItem(Object)
     * @see #addItemAfter(Object, Object)
     */
    V addItemBefore(T item, T before);

    /**
     * Adds multiple items to the data list.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * Any items that already present in the data provider are moved to the end.
     * <p>
     * Refreshes all items of the component after adding the items, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param items
     *            collection of item to add
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @see #removeItems(Collection)
     * @see #addItemsBefore(Collection, Object)
     * @see #addItemsAfter(Collection, Object)
     */
    V addItems(Collection<T> items);

    /**
     * Adds multiple items after the given target item. The full collection is
     * added in order after the target.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception. Any items that already present in
     * the data provider are moved.
     * <p>
     * Refreshes all items of the component after adding the item, i.e. runs
     * {@link DataView#refreshAll()}.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param items
     *            collection of items to add
     * @param after
     *            item after which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *             if item doesn't exist or collection is not a list
     * @see #addItems(Collection)
     * @see #addItemsBefore(Collection, Object)
     */
    V addItemsAfter(Collection<T> items, T after);

    /**
     * Adds multiple items before the given target item. The full collection is
     * added in order before the target.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * Any items that already present in the data provider are moved.
     * <p>
     * Refreshes all items of the component after adding the item, i.e. runs
     * {@link DataView#refreshAll()}.
     * <p>
     * Note! Item is added to the unfiltered and unsorted List.
     *
     * @param items
     *            collection of items to add
     * @param before
     *            item before which to add the item at
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @throws IllegalArgumentException
     *             if item doesn't exist or collection is not a list
     * @see #addItems(Collection)
     * @see #addItemsAfter(Collection, Object)
     */
    V addItemsBefore(Collection<T> items, T before);

    /**
     * Remove an item from the data list.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * Refreshes all items of the component after removing the item, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param item
     *            item to remove
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @see #addItem(Object)
     * @see #removeItems(Collection)
     */
    V removeItem(T item);

    /**
     * Remove multiple items from the data list.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     *
     * @param items
     *            collection of items to remove
     * @return this ListDataView instance
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @see #removeItem(Object)
     * @see #removeItems(Collection)
     */
    V removeItems(Collection<T> items);

    /**
     * Remove all the items in the list and adds given items to the data list.
     * <p>
     * The backing {@link List} must be mutable to use this method. Immutable
     * data structure will throw an exception.
     * <p>
     * Refreshes all items of the component after adding the items, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @throws UnsupportedOperationException
     *             if backing collection doesn't support modification
     * @param items
     *            collection of items to set
     * @return this ListDataView instance
     * @see #addItems(Collection)
     * @see #addItemsBefore(Collection, Object)
     * @see #addItemsAfter(Collection, Object)
     */
    V setItems(Collection<T> items);

    /**
     * Sets a filter to be applied to the data. The filter replaces any filter
     * that has been set or added previously. {@code null} will clear all
     * filters.
     * <p>
     * This filter is bound to the component. Thus, any other component using
     * the same {@link DataProvider} object would not be affected by setting a
     * filter through data view of another component. A filter set by this
     * method won't be retained when a new {@link DataProvider} is set to the
     * component.
     * <p>
     * Refreshes all items of the component after setting the filter, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     *
     * @param filter
     *            filter to be set, or <code>null</code> to clear any previously
     *            set filters
     * @return ListDataView instance
     *
     * @see #addFilter(SerializablePredicate)
     * @see #removeFilters()
     */
    V setFilter(SerializablePredicate<T> filter);

    /**
     * Adds a filter to be applied to all queries. The filter will be used in
     * addition to any filter that has been set or added previously.
     * <p>
     * This filter is bound to the component. Thus, any other component using
     * the same {@link DataProvider} object would not be affected by setting a
     * filter through data view of another component. A filter set by this
     * method won't be retained when a new {@link DataProvider} is set to the
     * component.
     * <p>
     * Refreshes all items of the component after adding the filter, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param filter
     *            the filter to add, not <code>null</code>
     * @return ListDataView instance
     *
     * @see #setFilter(SerializablePredicate)
     * @see #removeFilters()
     */
    V addFilter(SerializablePredicate<T> filter);

    /**
     * Removes all in-memory filters set or added.
     * <p>
     * Refreshes all items of the component after removing the filter, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @return ListDataView instance
     *
     * @see #addFilter(SerializablePredicate)
     * @see #setFilter(SerializablePredicate)
     */
    V removeFilters();

    /**
     * Sets the comparator to use as the default sorting. This overrides the
     * sorting set by any other method that manipulates the default sorting.
     * <p>
     * This comparator is bound to the component. Thus, any other component
     * using the same {@link DataProvider} object would not be affected by
     * setting a sort comparator through data view of another component. A
     * sorting set by this method won't be retained when a new
     * {@link DataProvider} is set to the component.
     * <p>
     * Refreshes all items of the component after setting the sorting, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param sortComparator
     *            a comparator to use, or <code>null</code> to clear any
     *            previously set sort order
     * @return ListDataView instance
     *
     * @see #addSortComparator(SerializableComparator)
     */
    V setSortComparator(SerializableComparator<T> sortComparator);

    /**
     * Adds a comparator to the data default sorting. If no default sorting has
     * been defined, then the provided comparator will be used as the default
     * sorting. If a default sorting has been defined, then the provided
     * comparator will be used to determine the ordering of items that are
     * considered equal by the previously defined default sorting.
     * <p>
     * This comparator is bound to the component. Thus, any other component
     * using the same {@link DataProvider} object would not be affected by
     * setting a sort comparator through data view of another component. A
     * sorting set by this method won't be retained when a new
     * {@link DataProvider} is set to the component.
     * <p>
     * Refreshes all items of the component after adding the sorting, i.e. runs
     * {@link DataView#refreshAll()}.
     *
     * @param sortComparator
     *            a comparator to add, not <code>null</code>
     * @return ListDataView instance
     *
     * @see #setSortComparator(SerializableComparator)
     */
    V addSortComparator(SerializableComparator<T> sortComparator);

    /**
     * Removes any default sorting that has been set or added previously.
     * <p>
     * Any other component using the same {@link DataProvider} object would not
     * be affected by removing default sorting through data view of another
     * component.
     * <p>
     * Refreshes all items of the component after removing the sorting, i.e.
     * runs {@link DataView#refreshAll()}.
     *
     * @return ListDataView instance
     *
     * @see #setSortComparator(SerializableComparator)
     * @see #addSortComparator(SerializableComparator)
     */
    V removeSorting();

    /**
     * Sets the property and direction to use as the default sorting. This
     * overrides the sorting set by any other method that manipulates the
     * default sorting of this {@link DataProvider}.
     * <p>
     * This sort order is bound to the component. Thus, any other component
     * using the same {@link DataProvider} object would not be affected by
     * setting a sort order through data view of another component. A sort order
     * set by this method won't be retained when a new {@link DataProvider} is
     * set to the component.
     * <p>
     * Refreshes all items of the component after setting the sorting, i.e. runs
     * {@link DataView#refreshAll()}.
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
     * Adds a property and direction to the default sorting. If no default
     * sorting has been defined, then the provided sort order will be used as
     * the default sorting. If a default sorting has been defined, then the
     * provided sort order will be used to determine the ordering of items that
     * are considered equal by the previously defined default sorting.
     * <p>
     * This sort order is bound to the component. Thus, any other component
     * using the same {@link DataProvider} object would not be affected by
     * setting a sort sort through data view of another component. A sorting set
     * by this method won't be retained when a new {@link DataProvider} is set
     * to the component.
     * <p>
     * Refreshes all items of the component after adding the sorting, i.e. runs
     * {@link DataView#refreshAll()}.
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
