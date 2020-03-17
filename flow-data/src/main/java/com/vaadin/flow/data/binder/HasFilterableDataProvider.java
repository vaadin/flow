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
package com.vaadin.flow.data.binder;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.SerializableFunction;

/**
 * A generic interface for listing components that use a filterable
 * {@link DataProvider} for showing data.
 * <p>
 * A listing component should implement either this interface or
 * {@link HasDataProvider}, but not both.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the item data type
 * @param <F>
 *            the filter type
 *
 * @see HasDataProvider
 */
public interface HasFilterableDataProvider<T, F> extends HasItems<T> {

    /**
     * Sets the {@link DataProvider} for this listing. The {@link DataProvider} is queried for
     * displayed items as needed.
     *
     * @param dataProvider
     *            the {@link DataProvider}, not <code>null</code>
     */
    default void setDataProvider(DataProvider<? extends T, F> dataProvider) {
        setDataProvider(dataProvider, SerializableFunction.identity());
    }

    /**
     * Sets the {@link DataProvider} and filter converter for this listing. The
     * {@link DataProvider} is queried for displayed items as needed.
     *
     * @param dataProvider
     *            the data provider, not <code>null</code>
     * @param filterConverter
     *            a function that converts filter values produced by this
     *            listing into filter values expected by the provided
     *            {@link DataProvider}, not <code>null</code>
     * @param <C>
     *            the filter type
     */
    <C> void setDataProvider(DataProvider<? extends T, C> dataProvider,
            SerializableFunction<F, C> filterConverter);
}
