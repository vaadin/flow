/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.SerializableFunction;

/**
 * A generic interface for listing components that use a filterable data
 * provider for showing data.
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
     * Sets the data provider for this listing. The data provider is queried for
     * displayed items as needed.
     *
     * @param dataProvider
     *            the data provider, not <code>null</code>
     */
    default void setDataProvider(DataProvider<T, F> dataProvider) {
        setDataProvider(dataProvider, SerializableFunction.identity());
    }

    /**
     * Sets the data provider and filter converter for this listing. The data
     * provider is queried for displayed items as needed.
     *
     * @param dataProvider
     *            the data provider, not <code>null</code>
     * @param filterConverter
     *            a function that converts filter values produced by this
     *            listing into filter values expected by the provided data
     *            provider, not <code>null</code>
     * @param <C>
     *            the filter type
     */
    <C> void setDataProvider(DataProvider<T, C> dataProvider,
            SerializableFunction<F, C> filterConverter);
}
