/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/**
 * A callback that is used to combine a new configured filter (given as a first
 * argument) and a component's current filter (given as a second argument).
 * 
 * @param <F>
 *            the filter type
 */
@FunctionalInterface
public interface FilterCombiner<F> extends SerializableBiFunction<F, F, F> {

    /**
     * Returns a filter combiner that always applies a new configured filter and
     * ignores a previously set filter.
     *
     * @param <F>
     *            the filter type
     * 
     * @return a filter combiner that always return a new filter as a result.
     */
    static <F> FilterCombiner<F> identity() {
        return (newFilter, previousFilter) -> newFilter;
    }
}
