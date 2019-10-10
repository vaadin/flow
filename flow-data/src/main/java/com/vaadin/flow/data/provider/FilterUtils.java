/*
 * Copyright 2000-2018 Vaadin Ltd.
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
     *
     * @param <C>
     *            the filter type that the wrapped data provider accepts;
     *            typically provided by a Component
     */
    public static <T, C, F> F convertFilter(
            SerializableFunction<C, F> filterConverter, Query<T, C> query) {
        return query.getFilter().map(filterConverter).orElse(null);
    }
}
