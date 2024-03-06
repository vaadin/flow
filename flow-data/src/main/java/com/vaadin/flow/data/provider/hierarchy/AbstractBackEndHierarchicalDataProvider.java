/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortOrder;

/**
 * Abstract base class for implementing
 * {@link BackEndHierarchicalDataProvider}s.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @since 1.2
 */
public abstract class AbstractBackEndHierarchicalDataProvider<T, F>
        extends AbstractHierarchicalDataProvider<T, F>
        implements BackEndHierarchicalDataProvider<T, F> {

    private List<QuerySortOrder> sortOrders = new ArrayList<>();

    private HierarchicalQuery<T, F> mixInSortOrders(
            HierarchicalQuery<T, F> query) {
        if (sortOrders.isEmpty()) {
            return query;
        }

        Set<String> sortedPropertyNames = query.getSortOrders().stream()
                .map(SortOrder::getSorted).collect(Collectors.toSet());

        List<QuerySortOrder> combinedSortOrders = Stream
                .concat(query.getSortOrders().stream(),
                        sortOrders.stream()
                                .filter(order -> !sortedPropertyNames
                                        .contains(order.getSorted())))
                .collect(Collectors.toList());

        return new HierarchicalQuery<>(query.getOffset(), query.getLimit(),
                combinedSortOrders, query.getInMemorySorting(),
                query.getFilter().orElse(null), query.getParent());
    }

    @Override
    public Stream<T> fetchChildren(HierarchicalQuery<T, F> query) {
        return fetchChildrenFromBackEnd(mixInSortOrders(query));
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public void setSortOrders(List<QuerySortOrder> sortOrders) {
        this.sortOrders = Objects.requireNonNull(sortOrders,
                "Sort orders cannot be null");
        refreshAll();
    }

    /**
     * Fetches data from the back end using the given query.
     *
     * @see HierarchicalQuery
     *
     * @param query
     *            the query that defines sorting, filtering, paging and the
     *            parent item to fetch children from
     * @return a stream of items matching the query
     */
    protected abstract Stream<T> fetchChildrenFromBackEnd(
            HierarchicalQuery<T, F> query);
}
