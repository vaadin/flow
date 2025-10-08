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

import com.vaadin.flow.function.SerializableBiFunction;

/**
 * A callback interface that is used to provide the index of an item.
 * <p>
 * Callback gives the target item and the query as parameters to fetch the
 * index. The index is the index of the item in the filtered and sorted data
 * set. If the item is not found, null is expected as a return value.
 * <p>
 * There will be inconsistent index if the data set for the returned index is
 * different from the component's data set. Changing the data set of either side
 * during this call may cause inconsistent index as a result.
 * <p>
 * Item index provider is only relevant with lazy data view implementations.
 *
 * @param <T>
 *            the type of the item
 * @param <F>
 *            the type of the query filter
 * @since @since 24.4
 */
@FunctionalInterface
public interface ItemIndexProvider<T, F>
        extends SerializableBiFunction<T, Query<T, F>, Integer> {
    /**
     * Gets the index of the item in the filtered and sorted data set.
     * <p>
     * There will be inconsistent index if the data set for the returned index
     * is different from the component's data set. Changing the data set of
     * either side during this call may cause inconsistent index as a result.
     * <p>
     * The query parameter provides a filter object being set with
     * {@link ConfigurableFilterDataProvider} or provided by a component, e.g. a
     * string filter in ComboBox.
     *
     * @param item
     *            Target item to get the index for
     * @param query
     *            Query prepared for fetching all items including filter and
     *            sorting.
     * @return the index of the item in the filtered and sorted data set, or
     *         null if not found
     */
    Integer apply(T item, Query<T, F> query);
}
