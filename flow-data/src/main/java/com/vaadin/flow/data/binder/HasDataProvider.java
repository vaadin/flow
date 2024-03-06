/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.Collection;

import com.vaadin.flow.data.provider.DataProvider;

/**
 * A generic interface for listing components that use a data provider for
 * showing data.
 * <p>
 * A listing component should implement either this interface or
 * {@link HasFilterableDataProvider}, but not both.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the item data type
 *
 * @see HasFilterableDataProvider
 */
public interface HasDataProvider<T> extends HasItems<T> {

    /**
     * Sets the data provider for this listing. The data provider is queried for
     * displayed items as needed.
     *
     * @param dataProvider
     *            the data provider, not null
     */
    void setDataProvider(DataProvider<T, ?> dataProvider);

    @Override
    default void setItems(Collection<T> items) {
        setDataProvider(DataProvider.ofCollection(items));
    }

}
