/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Internal filter related utilities for data provider.
 *
 * @author Vaadin Ltd
 * @since 1.3
 */
public final class FilterUtils {

    private FilterUtils() {
        // avoid instantiating utility class
    }

    /**
     * Combines the configured filter and the filter from the query into one
     * filter instance that can be passed to the wrapped data provider using the
     * {@code filterCombiner}.
     *
     * @param filterCombiner
     *            a filters combiner
     * @param queryFilter
     *            a query filter
     * @param configuredFilter
     *            a configured filter
     * @return a filters combination
     *
     * @param <F>
     *            the filter type of the wrapped data provider
     * @param <Q>
     *            the query filter type
     * @param <C>
     *            the configurable filter type
     */
    public static <F, Q, C> F combineFilters(
            SerializableBiFunction<Q, C, F> filterCombiner, Q queryFilter,
            C configuredFilter) {
        return filterCombiner.apply(queryFilter, configuredFilter);
    }

    /**
     * Gets the filter converted from a query filter by the
     * {@code filterConverter}.
     *
     * @param filterConverter
     *            callback that converts the filter in the query of the wrapped
     *            data provider into a filter supported by this data provider.
     *            Will only be called if the query contains a filter. Not
     *            <code>null</code>
     * @param query
     *            a query with a filter to convert
     * @return a converted filter, may be {@code null} if the query has no
     *         filter
     * @param <T>
     *            data type
     * @param <C>
     *            the filter type that the wrapped data provider accepts;
     *            typically provided by a Component
     * @param <F>
     *            the filter type of data provider
     */
    public static <T, C, F> F convertFilter(
            SerializableFunction<C, F> filterConverter, Query<T, C> query) {
        return query.getFilter().map(filterConverter).orElse(null);
    }
}
