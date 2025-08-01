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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.internal.Range;

/**
 * Mapper for hierarchical data.
 * <p>
 * Keeps track of the expanded nodes, and size of of the subtrees for each
 * expanded node.
 * <p>
 * This class is framework internal implementation details, and can be changed /
 * moved at any point. This means that you should not directly use this for
 * anything.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the data type
 * @param <F>
 *            the filter type
 * @since 1.2
 * @deprecated since 24.9 and will be removed in Vaadin 25.
 */
@Deprecated(since = "24.9", forRemoval = true)
public class HierarchyMapper<T, F> implements Serializable {

    // childMap is only used for finding parents of items and clean up on
    // removing children of expanded nodes.
    private Map<T, Set<T>> childMap = new HashMap<>();
    private Map<Object, T> parentIdMap = new HashMap<>();

    private final HierarchicalDataProvider<T, F> provider;
    private F filter;
    private List<QuerySortOrder> backEndSorting;
    private Comparator<T> inMemorySorting;

    private Map<Object, T> expandedItems = new HashMap<>();

    /**
     * Constructs a new HierarchyMapper.
     *
     * @param provider
     *            the hierarchical data provider for this mapper
     */
    public HierarchyMapper(HierarchicalDataProvider<T, F> provider) {
        this.provider = provider;
    }

    /**
     * Returns the size of the currently expanded hierarchy.
     *
     * @return the amount of available data
     */
    public int getTreeSize() {
        return (int) getHierarchy(null).count();
    }

    /**
     * Returns the size of root level.
     *
     * @return the amount of available root data
     */
    public int getRootSize() {
        return getDataProvider()
                .getChildCount(new HierarchicalQuery<>(filter, null));
    }

    /**
     * Finds the index of the parent of the item in given target index.
     *
     * @param item
     *            the item to get the parent of
     * @return the parent index or a negative value if the parent is not found
     *
     */
    public Integer getParentIndex(T item) {
        List<T> flatHierarchy = getHierarchy(null).collect(Collectors.toList());
        return flatHierarchy.indexOf(getParentOfItem(item));
    }

    /**
     * Finds the index of the item in active tree.
     *
     * @param item
     *            the target item
     * @return the index or a negative value if item is not found
     *
     */
    public Integer getIndex(T item) {
        List<T> flatHierarchy = getHierarchy(null).collect(Collectors.toList());
        return flatHierarchy.indexOf(item);
    }

    /**
     * Returns whether the given item is expanded.
     *
     * @param item
     *            the item to test
     * @return {@code true} if item is expanded; {@code false} if not
     */
    public boolean isExpanded(T item) {
        if (item == null) {
            // Root nodes are always visible.
            return true;
        }
        return expandedItems.containsKey(getDataProvider().getId(item));
    }

    /**
     * Expands the given item.
     *
     * @param item
     *            the item to expand
     * @return {@code true} if this method expanded the item, {@code false}
     *         otherwise
     */
    public boolean expand(T item) {
        return doExpand(item);
    }

    /**
     * Expands the given item.
     *
     * @param item
     *            the item to expand
     * @param position
     *            the index of the item
     * @return range of rows added by expanding the item
     */
    public Range expand(T item, Integer position) {
        if (doExpand(item) && position != null) {
            return Range.withLength(position + 1,
                    (int) getHierarchy(item, false).count());
        }

        return Range.withLength(0, 0);
    }

    /**
     * Expands the given item if it is collapsed and has children, and returns
     * whether this method expanded the item.
     *
     * @param item
     *            the item to expand
     * @return {@code true} if this method expanded the item, {@code false}
     *         otherwise
     */
    private boolean doExpand(T item) {
        boolean expanded = false;
        if (!isExpanded(item) && hasChildren(item)) {
            expandedItems.put(getDataProvider().getId(item), item);
            expanded = true;
        }
        return expanded;
    }

    /**
     * Collapses the given item.
     *
     * @param item
     *            the item to collapse
     * @return {@code true} if item has been collapsed, {@code false} if item is
     *         empty or already collapsed
     */
    public boolean collapse(T item) {
        if (item == null) {
            return false;
        }
        if (isExpanded(item)) {
            expandedItems.remove(getDataProvider().getId(item));
            return true;
        }
        return false;
    }

    /**
     * Collapses the given item.
     *
     * @param item
     *            the item to collapse
     * @param position
     *            the index of the item
     *
     * @return range of rows removed by collapsing the item
     */
    public Range collapse(T item, Integer position) {
        Range removedRows = Range.withLength(0, 0);
        if (isExpanded(item)) {
            if (position != null) {
                removedRows = Range.withLength(position + 1,
                        (int) getHierarchy(item, false).count());
            }
            expandedItems.remove(getDataProvider().getId(item));
        }
        return removedRows;
    }

    /**
     * Gets the current in-memory sorting.
     *
     * @return the in-memory sorting
     */
    public Comparator<T> getInMemorySorting() {
        return inMemorySorting;
    }

    /**
     * Sets the current in-memory sorting. This will cause the hierarchy to be
     * constructed again.
     *
     * @param inMemorySorting
     *            the in-memory sorting
     */
    public void setInMemorySorting(Comparator<T> inMemorySorting) {
        this.inMemorySorting = inMemorySorting;
    }

    /**
     * Gets the current back-end sorting.
     *
     * @return the back-end sorting
     */
    public List<QuerySortOrder> getBackEndSorting() {
        return backEndSorting;
    }

    /**
     * Sets the current back-end sorting. This will cause the hierarchy to be
     * constructed again.
     *
     * @param backEndSorting
     *            the back-end sorting
     */
    public void setBackEndSorting(List<QuerySortOrder> backEndSorting) {
        this.backEndSorting = backEndSorting;
    }

    /**
     * Gets the current filter.
     *
     * @return the filter
     */
    public F getFilter() {
        return filter;
    }

    /**
     * Sets the current filter. This will cause the hierarchy to be constructed
     * again.
     *
     * @param filter
     *            the filter
     */
    public void setFilter(Object filter) {
        this.filter = (F) filter;
    }

    /**
     * Gets the {@code HierarchicalDataProvider} for this
     * {@code HierarchyMapper}.
     *
     * @return the hierarchical data provider
     */
    public HierarchicalDataProvider<T, F> getDataProvider() {
        return provider;
    }

    /**
     * Returns whether given item has children.
     *
     * @param item
     *            the node to test
     * @return {@code true} if node has children; {@code false} if not
     */
    public boolean hasChildren(T item) {
        return getDataProvider().hasChildren(item);
    }

    /* Fetch methods. These are used to calculate what to request. */

    /**
     * Gets a stream of items in the form of a flattened hierarchy from the
     * back-end and filter the wanted results from the list.
     *
     * @param range
     *            the requested item range
     * @return the stream of items
     */
    public Stream<T> fetchHierarchyItems(Range range) {
        return getHierarchy(null).skip(range.getStart()).limit(range.length());
    }

    /**
     * Gets a stream of children for the given item in the form of a flattened
     * hierarchy from the back-end and filter the wanted results from the list.
     *
     * @param parent
     *            the parent item for the fetch
     * @param range
     *            the requested item range
     * @return the stream of items
     */
    public Stream<T> fetchHierarchyItems(T parent, Range range) {
        return getHierarchy(parent, false).skip(range.getStart())
                .limit(range.length());
    }

    /**
     * Gets a stream of root items from the back-end and filter the wanted
     * results from the list.
     *
     * @param range
     *            the requested item range
     * @return the stream of items
     */
    public Stream<T> fetchRootItems(Range range) {
        return getDirectChildren(null, range);
    }

    public Stream<T> fetchChildItems(T parent, Range range) {
        return getChildrenStream(parent, range, false);
    }

    public int countChildItems(T parent) {
        return getDataProvider()
                .getChildCount(new HierarchicalQuery<>(filter, parent));
    }

    /* Methods for providing information on the hierarchy. */

    /**
     * Generic method for finding direct children of a given parent, limited by
     * given range.
     *
     * @param parent
     *            the parent
     * @param range
     *            the range of direct children to return. null means full range.
     * @return the requested children of the given parent
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Stream<T> doFetchDirectChildren(T parent, Range range) {
        Range actualRange = (range == null)
                ? Range.withLength(0, Integer.MAX_VALUE)
                : range;
        return getDataProvider()
                .fetchChildren(new HierarchicalQuery(actualRange.getStart(),
                        actualRange.length(), getBackEndSorting(),
                        getInMemorySorting(), getFilter(), parent));
    }

    /**
     * Generic method for finding full range of direct children of a given
     * parent.
     *
     * @param parent
     *            the parent
     * @return the all children of the given parent
     */
    private Stream<T> doFetchDirectChildren(T parent) {
        return doFetchDirectChildren(parent, null);
    }

    /**
     * Returns depth of item in the tree starting from zero representing a root.
     *
     * @param item
     *            Target item
     * @return depth of item in the tree or -1 if item is null
     */
    public int getDepth(T item) {
        int depth = -1;
        while (item != null) {
            item = getParentOfItem(item);
            ++depth;
        }
        return depth;
    }

    /**
     * Find parent for the given item among open folders.
     *
     * @param item
     *            the item
     * @return parent item or {@code null} for root items or if the parent is
     *         closed
     */
    protected T getParentOfItem(T item) {
        Objects.requireNonNull(item, "Can not find the parent of null");
        return parentIdMap.get(getDataProvider().getId(item));
    }

    /**
     * Removes all children of an item identified by a given id. Items removed
     * by this method as well as the original item are all marked to be
     * collapsed. May be overridden in subclasses for removing obsolete data to
     * avoid memory leaks.
     *
     * @param id
     *            the item id
     */
    protected void removeChildren(Object id) {
        // Clean up removed nodes from child map
        Iterator<Entry<T, Set<T>>> iterator = childMap.entrySet().iterator();
        Set<T> invalidatedChildren = new HashSet<>();
        while (iterator.hasNext()) {
            Entry<T, Set<T>> entry = iterator.next();
            T key = entry.getKey();
            if (key != null && getDataProvider().getId(key).equals(id)) {
                invalidatedChildren.addAll(entry.getValue());
                iterator.remove();
            }
        }
        expandedItems.remove(id);
        invalidatedChildren.stream().map(getDataProvider()::getId)
                .forEach(x -> {
                    removeChildren(x);
                    parentIdMap.remove(x);
                });
    }

    /**
     * Finds the current index of given object. This is based on a search in
     * flattened version of the hierarchy.
     *
     * @param target
     *            the target object to find
     * @return optional index of given object
     */
    public Optional<Integer> getIndexOf(T target) {
        if (target == null) {
            return Optional.empty();
        }

        final List<Object> collect = getHierarchy(null).map(provider::getId)
                .collect(Collectors.toList());
        int index = collect.indexOf(getDataProvider().getId(target));
        return Optional.ofNullable(index < 0 ? null : index);
    }

    /**
     * Gets the full hierarchy tree starting from given node.
     *
     * @param parent
     *            the parent node to start from
     * @return the flattened hierarchy as a stream
     */
    private Stream<T> getHierarchy(T parent) {
        return getHierarchy(parent, true);
    }

    /**
     * Gets the full hierarchy tree starting from given node. The starting node
     * can be omitted.
     *
     * @param parent
     *            the parent node to start from
     * @param includeParent
     *            {@code true} to include the parent; {@code false} if not
     * @return the flattened hierarchy as a stream
     */
    private Stream<T> getHierarchy(T parent, boolean includeParent) {
        return Stream.of(parent)
                .flatMap(node -> getFlatChildrenStream(node, includeParent));
    }

    /**
     * Gets the stream of direct children for given node.
     *
     * @param parent
     *            the parent node
     * @param range
     * @return the stream of direct children
     */
    private Stream<T> getDirectChildren(T parent, Range range) {
        return getChildrenStream(parent, range, false);
    }

    /**
     * The method to recursively fetch the children of given parent. Used with
     * {@link Stream#flatMap} to expand a stream of parent nodes into a
     * flattened hierarchy.
     *
     * @param parent
     *            the parent node
     * @return the stream of all children under the parent, includes the parent
     */
    private Stream<T> getFlatChildrenStream(T parent) {
        return getFlatChildrenStream(parent, true);
    }

    /**
     * The method to recursively fetch the children of given parent. Used with
     * {@link Stream#flatMap} to expand a stream of parent nodes into a
     * flattened hierarchy.
     *
     * @param parent
     *            the parent node
     * @param includeParent
     *            {@code true} to include the parent in the stream;
     *            {@code false} if not
     * @return the stream of all children under the parent
     */
    private Stream<T> getFlatChildrenStream(T parent, boolean includeParent) {
        List<T> childList = Collections.emptyList();
        if (isExpanded(parent)) {
            try (Stream<T> stream = doFetchDirectChildren(parent)) {
                childList = stream.collect(Collectors.toList());
            }
            if (childList.isEmpty()) {
                removeChildren(parent == null ? null
                        : getDataProvider().getId(parent));
            } else {
                registerChildren(parent, childList);
            }
        }
        return combineParentAndChildStreams(parent,
                childList.stream().flatMap(this::getFlatChildrenStream),
                includeParent);
    }

    /**
     * The method fetch the children of given parent.
     *
     * @param parent
     *            the parent node
     * @param range
     * @param includeParent
     *            {@code true} to include the parent in the stream;
     *            {@code false} if not
     * @return the stream of all direct children under the parent
     */
    private Stream<T> getChildrenStream(T parent, Range range,
            boolean includeParent) {
        List<T> childList = Collections.emptyList();
        if (isExpanded(parent)) {
            try (Stream<T> stream = doFetchDirectChildren(parent, range)) {
                childList = stream.collect(Collectors.toList());
            }
            if (childList.isEmpty()) {
                removeChildren(parent == null ? null
                        : getDataProvider().getId(parent));
            } else {
                registerChildren(parent, childList);
            }
        }
        return combineParentAndChildStreams(parent, childList.stream(),
                includeParent);
    }

    /**
     * Register parent and children items into inner structures. May be
     * overridden in subclasses.
     *
     * @param parent
     *            the parent item
     * @param childList
     *            list of parents children to be registered.
     */
    protected void registerChildren(T parent, List<T> childList) {
        childMap.put(parent, new HashSet<>(childList));
        childList.forEach(
                x -> parentIdMap.put(getDataProvider().getId(x), parent));
    }

    /**
     * Helper method for combining parent and a stream of children into one
     * stream. {@code null} item is never included, and parent can be skipped by
     * providing the correct value for {@code includeParent}.
     *
     * @param parent
     *            the parent node
     * @param children
     *            the stream of children
     * @param includeParent
     *            {@code true} to include the parent in the stream;
     *            {@code false} if not
     * @return the combined stream of parent and its children
     */
    private Stream<T> combineParentAndChildStreams(T parent, Stream<T> children,
            boolean includeParent) {
        boolean parentIncluded = includeParent && parent != null;
        Stream<T> parentStream = parentIncluded ? Stream.of(parent)
                : Stream.empty();
        return Stream.concat(parentStream, children);
    }

    public void destroyAllData() {
        childMap.clear();
        parentIdMap.clear();
        expandedItems.clear();
    }

    /**
     * Returns true if there is any expanded items.
     *
     * @return {@code true} if there is any expanded items.
     */
    public boolean hasExpandedItems() {
        return !expandedItems.isEmpty();
    }

    /**
     * Returns the expanded items in form of an unmodifiable collection.
     *
     * @return an unmodifiable {@code Collection<T>} containing the expanded
     *         items.
     */
    public Collection<T> getExpandedItems() {
        return Collections.unmodifiableCollection(expandedItems.values());
    }

    /**
     * Returns true if the given item is on any currently active child list.
     *
     * @param item
     *            Item to test
     * @return {@literal true} if item is an active child, {@literal false}
     *         otherwise.
     */
    boolean hasCurrentlyActiveChild(T item) {
        for (Set<T> children : childMap.values()) {
            if (children.contains(item)) {
                return true;
            }
        }
        return false;
    }
}
