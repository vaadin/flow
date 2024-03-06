/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract base class for implementing back end data providers.
 *
 * @param <T>
 *            data provider data type
 * @param <F>
 *            data provider filter type
 * @since 1.0
 */
public abstract class AbstractBackEndDataProvider<T, F> extends
        AbstractDataProvider<T, F> implements BackEndDataProvider<T, F> {

    private List<QuerySortOrder> sortOrders = new ArrayList<>();

    private Query<T, F> mixInSortOrders(Query<T, F> query) {
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

        return new Query<>(query.getOffset(), query.getLimit(),
                combinedSortOrders, query.getInMemorySorting(),
                query.getFilter().orElse(null));
    }

    @Override
    public Stream<T> fetch(Query<T, F> query) {
        return fetchFromBackEnd(mixInSortOrders(query));
    }

    @Override
    public int size(Query<T, F> query) {
        return sizeInBackEnd(mixInSortOrders(query));
    }

    /**
     * Fetches data from the back end using the given query.
     *
     * @param query
     *            the query that defines sorting, filtering and paging for
     *            fetching the data
     * @return a stream of items matching the query
     */
    protected abstract Stream<T> fetchFromBackEnd(Query<T, F> query);

    /**
     * Counts the number of items available in the back end.
     *
     * @param query
     *            the query that defines filtering to be used for counting the
     *            number of items
     * @return the number of available items
     */
    protected abstract int sizeInBackEnd(Query<T, F> query);

    @Override
    public void setSortOrders(List<QuerySortOrder> sortOrders) {
        this.sortOrders = Objects.requireNonNull(sortOrders,
                "Sort orders cannot be null");
        refreshAll();
    }

}
