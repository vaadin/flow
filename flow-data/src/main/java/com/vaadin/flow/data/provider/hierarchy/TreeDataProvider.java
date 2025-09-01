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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * An in-memory data provider for listing components that display hierarchical
 * data. Uses an instance of {@link TreeData} as its source of data.
 *
 * @author Vaadin Ltd
 * @since 1.2
 *
 * @param <T>
 *            data type
 */
public class TreeDataProvider<T>
        extends AbstractHierarchicalDataProvider<T, SerializablePredicate<T>>
        implements InMemoryDataProvider<T> {

    private final TreeData<T> treeData;

    private SerializablePredicate<T> filter = null;

    private SerializableComparator<T> sortOrder = null;

    private HierarchyFormat hierarchyFormat = HierarchyFormat.NESTED;

    /**
     * Constructs a new TreeDataProvider.
     * <p>
     * The data provider should be refreshed after making changes to the
     * underlying {@link TreeData} instance.
     *
     * @param treeData
     *            the backing {@link TreeData} for this provider, not
     *            {@code null}
     */
    public TreeDataProvider(TreeData<T> treeData) {
        Objects.requireNonNull(treeData, "treeData cannot be null");
        this.treeData = treeData;
    }

    /**
     * Creates a new TreeDataProvider and configures it to return the
     * hierarchical data in the specified format: {@link HierarchyFormat#NESTED}
     * or {@link HierarchyFormat#FLATTENED}.
     * <p>
     * The data provider should be refreshed after making changes to the
     * underlying {@link TreeData} instance.
     *
     * @param treeData
     *            the backing {@link TreeData} for this provider, not
     *            {@code null}
     * @param hierarchyFormat
     *            the hierarchy format to return data in
     */
    public TreeDataProvider(TreeData<T> treeData,
            HierarchyFormat hierarchyFormat) {
        this(treeData);
        this.hierarchyFormat = hierarchyFormat;
    }

    @Override
    public HierarchyFormat getHierarchyFormat() {
        return hierarchyFormat;
    }

    /**
     * Return the underlying hierarchical data of this provider.
     *
     * @return the underlying data of this provider
     */
    public TreeData<T> getTreeData() {
        return treeData;
    }

    @Override
    public boolean hasChildren(T item) {
        if (!treeData.contains(item)) {
            // The item might be dropped from the tree already
            return false;
        }
        return !treeData.getChildren(item).isEmpty();
    }

    @Override
    public int getDepth(T item) {
        int depth = 0;
        while ((item = treeData.getParent(item)) != null) {
            depth++;
        }
        return depth;
    }

    @Override
    public int getChildCount(
            HierarchicalQuery<T, SerializablePredicate<T>> query) {
        Optional<SerializablePredicate<T>> combinedFilter = getCombinedFilter(
                query.getFilter());

        return (int) flatten(query.getParent(), query.getExpandedItemIds(),
                combinedFilter, Optional.empty()).stream()
                .skip(query.getOffset()).limit(query.getLimit()).count();
    }

    @Override
    public Stream<T> fetchChildren(
            HierarchicalQuery<T, SerializablePredicate<T>> query) {
        if (!treeData.contains(query.getParent())) {
            throw new IllegalArgumentException("The queried item "
                    + query.getParent()
                    + " could not be found in the backing TreeData. "
                    + "Did you forget to refresh this data provider after item removal?");
        }

        Optional<SerializablePredicate<T>> combinedFilter = getCombinedFilter(
                query.getFilter());

        Optional<Comparator<T>> comparator = Stream
                .of(query.getInMemorySorting(), sortOrder)
                .filter(Objects::nonNull)
                .reduce((c1, c2) -> c1.thenComparing(c2));

        return flatten(query.getParent(), query.getExpandedItemIds(),
                combinedFilter, comparator).stream().skip(query.getOffset())
                .limit(query.getLimit());
    }

    @Override
    public SerializablePredicate<T> getFilter() {
        return filter;
    }

    @Override
    public void setFilter(SerializablePredicate<T> filter) {
        this.filter = filter;
        refreshAll();
    }

    @Override
    public SerializableComparator<T> getSortComparator() {
        return sortOrder;
    }

    @Override
    public void setSortComparator(SerializableComparator<T> comparator) {
        sortOrder = comparator;
        refreshAll();
    }

    private Optional<SerializablePredicate<T>> getCombinedFilter(
            Optional<SerializablePredicate<T>> queryFilter) {
        return filter != null
                ? Optional.of(queryFilter.map(filter::and).orElse(filter))
                : queryFilter;
    }

    private List<T> flatten(T parent, Set<Object> expandedItemIds,
            Optional<SerializablePredicate<T>> combinedFilter,
            Optional<Comparator<T>> comparator) {
        List<T> result = new ArrayList<>();

        List<T> children = new ArrayList<>(getTreeData().getChildren(parent));

        if (comparator.isPresent()) {
            children.sort(comparator.get());
        }

        for (T child : children) {
            List<T> descendants;

            if (expandedItemIds.contains(getId(child))) {
                descendants = flatten(child, expandedItemIds, combinedFilter,
                        comparator);
            } else {
                descendants = new ArrayList<>();
            }

            if (combinedFilter.map(f -> f.test(child)).orElse(true)
                    || descendants.size() > 0) {
                result.add(child);
                result.addAll(descendants);
            }
        }

        return result;
    }
}
