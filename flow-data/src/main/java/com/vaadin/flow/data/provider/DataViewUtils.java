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

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * Internal utility class used by data view implementations and components to
 * simplify the filtering and sorting handling, but not limited to it.
 *
 * @author Vaadin Ltd
 */
public final class DataViewUtils {

    private static final String COMPONENT_IN_MEMORY_FILTER_KEY = "component-in-memory-filter-key";
    private static final String COMPONENT_IN_MEMORY_SORTING_KEY = "component-in-memory-sorting-key";

    private DataViewUtils() {
        // avoid instantiating utility class
    }

    /**
     * Gets the in-memory filter of a given component instance.
     *
     * @param component
     *            component instance the filter is bound to
     * @param <T>
     *            item type
     * @return optional component's in-memory filter.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<SerializablePredicate<T>> getComponentFilter(
            Component component) {
        return Optional.ofNullable((SerializablePredicate<T>) ComponentUtil
                .getData(component, COMPONENT_IN_MEMORY_FILTER_KEY));
    }

    /**
     * Gets the in-memory sort comparator of a given component instance.
     *
     * @param component
     *            component instance the sort comparator is bound to
     * @param <T>
     *            item type
     * @return optional component's in-memory sort comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<SerializableComparator<T>> getComponentSortComparator(
            Component component) {
        return Optional.ofNullable((SerializableComparator<T>) ComponentUtil
                .getData(component, COMPONENT_IN_MEMORY_SORTING_KEY));
    }

    /**
     * Sets the in-memory filter to a given component instance. The filter
     * replaces any filter that has been set or added previously. {@code null}
     * will clear all filters.
     *
     * @param component
     *            component instance the filter is bound to
     * @param filter
     *            component's in-memory filter to be set, or <code>null</code>
     *            to clear any previously set filters
     * @param <T>
     *            items type
     */
    public static <T> void setComponentFilter(Component component,
            SerializablePredicate<T> filter) {
        ComponentUtil.setData(component, COMPONENT_IN_MEMORY_FILTER_KEY,
                filter);
    }

    /**
     * Sets the in-memory sort comparator to a given component instance. The
     * sort comparator replaces any sort comparator that has been set or added
     * previously. {@code null} will clear all sort comparators.
     *
     * @param component
     *            component instance the sort comparator is bound to
     * @param sortComparator
     *            component's in-memory sort comparator to be set, or
     *            <code>null</code> to clear any previously set sort comparators
     * @param <T>
     *            items type
     */
    public static <T> void setComponentSortComparator(Component component,
            SerializableComparator<T> sortComparator) {
        ComponentUtil.setData(component, COMPONENT_IN_MEMORY_SORTING_KEY,
                sortComparator);
    }

    /**
     * Removes the in-memory filter and sort comparator from a given component
     * instance.
     *
     * @param component
     *            component instance the filter and sort comparator are removed
     *            from
     */
    public static void removeComponentFilterAndSortComparator(
            Component component) {
        setComponentFilter(component, null);
        setComponentSortComparator(component, null);
    }

    /**
     * Generates a data query with component's in-memory filter and sort
     * comparator.
     *
     * @param component
     *            component instance the filter and sort comparator are bound to
     * @return query instance
     */
    @SuppressWarnings({ "rawtypes" })
    public static Query getQuery(Component component) {
        return getQuery(component, true);
    }

    /**
     * Generates a data query with component's in-memory filter and sort
     * comparator, which is optionally included if {@code withSorting} is set to
     * {@code true}.
     *
     * @param component
     *            component instance the filter and sort comparator are bound to
     * @param withSorting
     *            if {@code true}, the component's sort comparator will be
     *            included in the query.
     * @return query instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Query getQuery(Component component, boolean withSorting) {
        final Optional<SerializablePredicate<Object>> filter = DataViewUtils
                .getComponentFilter(component);

        final Optional<SerializableComparator<Object>> sorting = withSorting
                ? DataViewUtils.getComponentSortComparator(component)
                : Optional.empty();

        return new Query(0, Integer.MAX_VALUE, null, sorting.orElse(null),
                filter.orElse(null));
    }
}
