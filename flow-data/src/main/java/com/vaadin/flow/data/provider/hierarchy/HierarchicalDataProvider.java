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
package com.vaadin.flow.data.provider.hierarchy;

import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.FilterUtils;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalFilterUtils.HierarchialConfigurableFilterDataProviderWrapper;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalFilterUtils.HierarchicalFilterDataProviderWrapper;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;

/**
 * A common interface for fetching hierarchical data from a data source, such as
 * an in-memory collection or a backend database. It supports two hierarchy
 * formats: {@link HierarchyFormat#NESTED} and
 * {@link HierarchyFormat#FLATTENED}.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @since 1.2
 */
public interface HierarchicalDataProvider<T, F> extends DataProvider<T, F> {
    public enum HierarchyFormat {
        /**
         * The nested hierarchy format refers to a data provider implementation
         * in which each expanded parent's children are fetched via a separate
         * request, on demand:
         *
         * <pre>
         * └── Item 0               | Fetched in 1st request
         *     └── Item 0-0         | Fetched in 2nd request
         *         └── Item 0-0-0   | Fetched in 3rd request
         * └── Item 1               | Fetched in 1st request
         * </pre>
         *
         * Every request to the data provider returns a paginated list that
         * contains only the direct children of the
         * {@link HierarchicalQuery#getParent() requested parent}. The component
         * decides when to request deeper levels and how much to load based on
         * the current viewport and the expansion state of items, storing the
         * loaded hierarchy state in memory.
         * <p>
         * Example:
         *
         * <pre>
         * class MyDataProvider
         *         implements HierarchicalDataProvider&lt;String, Void&gt; {
         *     private HashMap&lt;String, List&lt;String&gt;&gt; data = new HashMap&lt;&gt;() {
         *         {
         *             put(null, List.of("Item 0", "Item 1"));
         *             put("Item 0", List.of("Item 0-0"));
         *             put("Item 0-0", List.of("Item 0-0-0"));
         *         }
         *     };
         *
         *     &#64;Override
         *     public HierarchyFormat getHierarchyFormat() {
         *         return HierarchyFormat.NESTED;
         *     }
         *
         *     &#64;Override
         *     public Stream&lt;String&gt; fetchChildren(
         *             HierarchicalQuery&lt;String, Void&gt; query) {
         *         return data.get(query.getParent()).stream()
         *                 .skip(query.getOffset()).limit(query.getLimit());
         *     }
         *
         *     &#64;Override
         *     public int getChildCount(HierarchicalQuery&lt;String, Void&gt; query) {
         *         return data.get(query.getParent()).size();
         *     }
         * }
         * </pre>
         *
         * Pros:
         * <ul>
         * <li>Simple and fast data queries by avoiding hierarchy construction –
         * each request fetches only the direct children, which are then cached
         * hierarchically.
         * </ul>
         *
         * Cons:
         * <ul>
         * <li>The full size and structure of the tree remains unknown without
         * recursively fetching the entire hierarchy, which is impractical due
         * to potentially a lot of consecutive requests and heavy memory usage.
         * As a result, the scroll position cannot be restored automatically
         * after using {@link HierarchicalDataProvider#refreshAll()} which
         * resets the cached hierarchy state.
         * <li>The scroll container size updates dynamically while the user
         * scrolls, which can cause them to jump over and miss some levels when
         * scrolling quickly.
         * </ul>
         */
        NESTED,

        /**
         * The flattened hierarchy format refers to a data provider
         * implementation that returns the entire subtree of the
         * {@link HierarchicalQuery#getParent() requested parent} in a single,
         * flattened, paginated list:
         *
         * <pre>
         * └── Item 0
         * └── Item 0-0
         * └── Item 0-0-0
         * └── Item 1
         * </pre>
         *
         * The list contains all expanded descendants, arranged in depth-first
         * order: starting from the parent, then its children and their
         * descendants, before the next sibling. The component delegates
         * hierarchy construction entirely to the data provider by supplying a
         * {@link HierarchicalQuery} where:
         * <ul>
         * <li>{@link HierarchicalQuery#getOffset()} points to the start of the
         * requested range across all flattened descendants
         * <li>{@link HierarchicalQuery#getLimit()} specifies the number of
         * items to return
         * <li>{@link HierarchicalQuery#getExpandedItemIds()} contains the set
         * of expanded item IDs
         * </ul>
         * The flattened format also requires the data provider to implement
         * {@link HierarchicalDataProvider#getDepth(T)}, which the component
         * uses to make items appear as a hierarchy by applying visual
         * indentation.
         * <p>
         * Example:
         *
         * <pre>
         * class MyDataProvider
         *         implements HierarchicalDataProvider&lt;String, Void&gt; {
         *     private HashMap&lt;String, List&lt;String&gt;&gt; data = new HashMap&lt;>() {
         *         {
         *             put(null, List.of("Item 0", "Item 1"));
         *             put("Item 0", List.of("Item 0-0"));
         *             put("Item 0-0", List.of("Item 0-0-0"));
         *         }
         *     };
         *
         *     &#64;Override
         *     public HierarchyFormat getHierarchyFormat() {
         *         return HierarchyFormat.FLATTENED;
         *     }
         *
         *     &#64;Override
         *     public Stream&lt;String&gt; fetchChildren(
         *             HierarchicalQuery&lt;String, Void&gt; query) {
         *         return flatten(query.getParent(), query.getExpandedItemIds())
         *                 .skip(query.getOffset()).limit(query.getLimit());
         *     }
         *
         *     &#64;Override
         *     public int getChildCount(HierarchicalQuery&lt;String, Void&gt; query) {
         *         return (int) flatten(query.getParent(),
         *                 query.getExpandedItemIds()).count();
         *     }
         *
         *     &#64;Override
         *     public int getDepth(String item) {
         *         return item.split("-").length - 1;
         *     }
         *
         *     private Stream&lt;String&gt; flatten(String parent,
         *             Set&lt;Object&gt; expandedItemIds) {
         *         return data.getOrDefault(parent, List.of()).stream().flatMap(
         *                 child -> expandedItemIds.contains(getId(child))
         *                         ? Stream.concat(Stream.of(child),
         *                                 flatten(child, expandedItemIds))
         *                         : Stream.of(child));
         *     }
         * }
         * </pre>
         *
         * Pros:
         * <ul>
         * <li>The component can fetch the total size of the tree upfront and
         * set a fixed scroll container size that does not change while
         * scrolling, resulting in more predictable behavior.
         * <li>{@link #refreshAll()} avoids unexpected scroll jumps by
         * refetching the total tree size and retaining the size of expanded
         * items whose nested structure hasn't changed.
         * <li>The developer has full control over how data is queried, making
         * it possible to leverage more advanced optimizations and storage
         * strategies at the database level.
         * </ul>
         *
         * Cons:
         * <ul>
         * <li>Increased complexity and potentially heavier data queries due to
         * the need for hierarchy reconstruction, which may require, for
         * example, the use of recursive CTEs (Common Table Expressions) to
         * retrieve all descendants of an item in a single SQL query.
         * </ul>
         *
         * @since 25.0
         */
        FLATTENED,
    }

    /**
     * Specifies the format in which the data provider returns hierarchical
     * data. The default format is {@link HierarchyFormat#NESTED}.
     * <p>
     * The component uses this method to determine how to fetch and render
     * hierarchical data with this data provider.
     *
     * @since 25.0
     * @return the hierarchy format
     */
    default public HierarchyFormat getHierarchyFormat() {
        return HierarchyFormat.NESTED;
    }

    /**
     * Gets the number of children based on the given hierarchical query.
     * <p>
     * The behavior of this method depends on the implemented hierarchy format,
     * see {@link #getHierarchyFormat()} and
     * {@link #getChildCount(HierarchicalQuery)}
     *
     * @param query
     *            given query to request the count for
     * @return the count of children for the parent item
     *         {@link HierarchicalQuery#getParent()} or the root level if
     *         {@code null}
     * @throws IllegalArgumentException
     *             if the query is not of type HierarchicalQuery
     */
    @Override
    public default int size(Query<T, F> query) {
        if (query instanceof HierarchicalQuery<?, ?>) {
            return getChildCount((HierarchicalQuery<T, F>) query);
        }
        throw new IllegalArgumentException(
                "Hierarchical data provider doesn't support non-hierarchical queries");
    }

    /**
     * Fetches children based on the given hierarchical query.
     * <p>
     * The behavior of this method depends on the implemented hierarchy format,
     * see {@link #getHierarchyFormat()} and
     * {@link #fetchChildren(HierarchicalQuery)}
     *
     * @param query
     *            given query to request data with
     * @return a stream of data objects for the
     *         {@link HierarchicalQuery#getParent() parent item} or the root
     *         level if the parent is {@code null}, must not contain null values
     * @throws IllegalArgumentException
     *             if the query is not of type HierarchicalQuery
     */
    @Override
    public default Stream<T> fetch(Query<T, F> query) {
        if (query instanceof HierarchicalQuery<?, ?>) {
            return fetchChildren((HierarchicalQuery<T, F>) query);
        }
        throw new IllegalArgumentException(
                "Hierarchical data provider doesn't support non-hierarchical queries");
    }

    /**
     * Gets the number of children based on the given hierarchical query.
     * <p>
     * This method must be implemented in accordance with the selected hierarchy
     * type, see {@link #getHierarchyFormat()} and {@link HierarchyFormat}.
     *
     * @param query
     *            given query to request the count for
     * @return the count of children for the parent item or the root level if
     *         the parent is {@code null}
     */
    public int getChildCount(HierarchicalQuery<T, F> query);

    /**
     * Fetches children based on the given hierarchical query.
     * <p>
     * This method must be implemented in accordance with the selected hierarchy
     * type, see {@link #getHierarchyFormat()} and {@link HierarchyFormat}.
     *
     * @param query
     *            given query to request data with
     * @return a stream of data objects for the
     *         {@link HierarchicalQuery#getParent() parent item} or the root
     *         level if the parent is {@code null}, must not contain null values
     */
    public Stream<T> fetchChildren(HierarchicalQuery<T, F> query);

    /**
     * Check whether a given item has any children associated with it.
     *
     * @param item
     *            the item to check for children
     * @return whether the given item has children
     */
    public boolean hasChildren(T item);

    /**
     * Gets the parent item for the given item.
     *
     * @param item
     *            the item for which to retrieve the parent item for
     * @return parent item for the given item or {@code null} if the item is a
     *         root item
     * @throws UnsupportedOperationException
     *             if not implemented
     */
    default T getParent(T item) {
        throw new UnsupportedOperationException(
                "The getParent method is not implemented for this data provider");
    }

    /**
     * Gets the index of a given item based on the given hierarchical query.
     *
     * @param item
     *            the item to get the index for
     * @param query
     *            given query to request data with
     * @return the index of the provided item or -1 if not found
     * @throws UnsupportedOperationException
     *             if not implemented
     */
    default int getItemIndex(T item, HierarchicalQuery<T, F> query) {
        if (!(this instanceof InMemoryDataProvider)) {
            throw new UnsupportedOperationException(
                    "The getItemIndex method is not implemented for this data provider");
        }
        Objects.requireNonNull(item, "Item cannot be null");
        Objects.requireNonNull(query, "Query cannot be null");
        return fetchChildren(query).map(this::getId).toList().indexOf(getId(item));
    }

    /**
     * Gets the depth of a given item in the hierarchy, starting from zero
     * (root).
     * <p>
     * This method must be implemented for data providers that implement the
     * flattened hierarchy format, see {@link #getHierarchyFormat()} and
     * {@link HierarchyFormat#FLATTENED}.
     *
     * @param item
     *            the item to get the depth for
     * @return the depth of the item in the hierarchy
     * @throws UnsupportedOperationException
     *             if not implemented
     */
    default public int getDepth(T item) {
        if (HierarchyFormat.FLATTENED.equals(getHierarchyFormat())) {
            throw new UnsupportedOperationException(
                    """
                            The getDepth method must be implemented when getHierarchyFormat() \
                            is configured as HierarchyFormat#FLATTENED
                            """);
        }

        throw new UnsupportedOperationException(
                "The getDepth method is not implemented for this data provider");
    }

    @SuppressWarnings("serial")
    @Override
    default <Q, C> HierarchicalConfigurableFilterDataProvider<T, Q, C> withConfigurableFilter(
            SerializableBiFunction<Q, C, F> filterCombiner) {
        return new HierarchialConfigurableFilterDataProviderWrapper<T, Q, C, F>(
                this) {
            @Override
            protected F combineFilters(Q queryFilter, C configuredFilter) {
                return FilterUtils.combineFilters(filterCombiner, queryFilter,
                        configuredFilter);
            }
        };
    }

    @Override
    @SuppressWarnings("serial")
    default <C> HierarchicalDataProvider<T, C> withConvertedFilter(
            SerializableFunction<C, F> filterConverter) {
        return new HierarchicalFilterDataProviderWrapper<T, C, F>(this) {
            @Override
            protected F getFilter(Query<T, C> query) {
                return FilterUtils.convertFilter(filterConverter, query);
            }
        };
    }

    @Override
    default HierarchicalConfigurableFilterDataProvider<T, Void, F> withConfigurableFilter() {
        return (HierarchicalConfigurableFilterDataProvider<T, Void, F>) DataProvider.super.withConfigurableFilter();
    }
}
