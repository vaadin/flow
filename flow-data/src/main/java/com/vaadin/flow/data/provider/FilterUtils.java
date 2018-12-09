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
 *
 */
public final class FilterUtils {

    private FilterUtils() {
        // avoid instantiating utility class
    }

    public static <F, Q, C> F combineFilters(
            SerializableBiFunction<Q, C, F> filterCombiner, Q queryFilter,
            C configuredFilter) {
        return filterCombiner.apply(queryFilter, configuredFilter);
    }

    public static <T, C, F> F convertFilter(
            SerializableFunction<C, F> filterConverter, Query<T, C> query) {
        return query.getFilter().map(filterConverter).orElse(null);
    }
}
