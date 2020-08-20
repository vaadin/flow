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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * An interface for components that get items from the generic data provider
 * types {@link DataProvider} and {@link InMemoryDataProvider}. The methods
 * return a {@link DataView} which has the generic API for getting information
 * on the items.
 *
 * @param <T>
 *            data type
 * @param <V>
 *            DataView type
 * @since
 */
public interface HasDataView<T, F, V extends DataView<T>> extends Serializable {

    /**
     * Set a generic data provider for the component to use and returns the base
     * {@link DataView} that provides API to get information on the items.
     * <p>
     * This method should be used only when the data provider type is not either
     * {@link ListDataProvider} or {@link BackEndDataProvider}.
     *
     * @param dataProvider
     *            DataProvider instance to use, not <code>null</code>
     * @return DataView providing information on the data
     */
    V setItems(DataProvider<T, F> dataProvider);

    /**
     * Sets an in-memory data provider for the component to use, taking into
     * account both in-memory filtering from data provider and component
     * specific internal filter.
     * <p>
     * Component's filter is transformed into a predicate through the given
     * filter combiner. Example of filter combiner which produces the
     * Person's name predicate:
     * {@code (String nameFilter) -> person -> person.getName().equalsIgnoreCase
     * (nameFilter);}
     * <p>
     * Note! Using a {@link ListDataProvider} instead of a
     * {@link InMemoryDataProvider} is recommended to get access to
     * {@link ListDataView} API by using
     * {@link HasListDataView#setItems(ListDataProvider)}.
     *
     * @param dataProvider
     *            InMemoryDataProvider to use, not <code>null</code>
     * @param filterConverter a function which converts a component's
     *                        internal filter into a predicate to be
     *                        applied to the all items of data provider
     * @return DataView providing information on the data
     *
     * @see #setItems(InMemoryDataProvider)
     */
    default V setItems(InMemoryDataProvider<T> dataProvider,
            SerializableFunction<F, SerializablePredicate<T>> filterConverter) {
        Objects.requireNonNull(filterConverter,
                "FilterConverter cannot be null");
        DataProvider<T, F> convertedDataProvider = dataProvider
                .withConvertedFilter(filter -> Optional
                        .ofNullable(dataProvider.getFilter())
                        .orElse(item -> true)
                        .and(item -> filterConverter.apply(filter).test(item)));
        return setItems(convertedDataProvider);
    }

    /**
     * Sets an in-memory data provider for the component to use, taking into
     * account only in-memory filtering from data provider.
     * <p>
     * This methods ignores the component specific filter, even if it's included
     * into the query object. If you want to take it into account, please use
     * {@link #setItems(InMemoryDataProvider, SerializableFunction)}.
     * <p>
     * Note! Using a {@link ListDataProvider} instead of a
     * {@link InMemoryDataProvider} is recommended to get access to
     * {@link ListDataView} API by using
     * {@link HasListDataView#setItems(ListDataProvider)}.
     *
     * @param dataProvider
     *            InMemoryDataProvider to use, not <code>null</code>
     * @return DataView providing information on the data
     *
     * @see #setItems(InMemoryDataProvider, SerializableFunction)
     */
    // TODO: probably we should just remove this methods, because it has a
    //  significant drawback: it ignores the component's filter quietly, even
    //  though it described in javadoc, so the developer can easily make a
    //  mistake here.
    //
    // After the filter type has been added to this mixin, it's now
    // impossible to pass the custom in-memory data provider to
    // setItems(DataProvider<T, F> dataProvider), unless the
    // F=SerializablePredicate. So, anyway we have to ask the developer how
    // does he want to resolve the filter conversion.
    default V setItems(InMemoryDataProvider<T> dataProvider) {
        return setItems(dataProvider, ignore -> item -> true);
    }

    /**
     * Get the DataView for the component.
     * <p>
     * The returned DataView only contains a minimal common API. Use of
     * {@link HasListDataView#getListDataView} or
     * {@link HasLazyDataView#getLazyDataView} should be used for more targeted
     * helper features
     *
     * @return DataView instance
     */
    V getGenericDataView();
}
