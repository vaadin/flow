/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;
import java.util.Collection;

/**
 * An interface for components that accept setting items in-memory and returns a
 * {@link ListDataView} that provides information and allows operations on the
 * items.
 *
 * @param <T>
 *            item type
 * @param <V>
 *            DataView type
 * @since
 */
public interface HasListDataView<T, V extends ListDataView<T, ?>>
        extends Serializable {
    /**
     * Sets a ListDataProvider for the component to use and returns a
     * {@link ListDataView} that provides information and allows operations on
     * the items.
     *
     * @param dataProvider
     *            ListDataProvider providing items to the component.
     * @return ListDataView providing access to the items
     */
    V setItems(ListDataProvider<T> dataProvider);

    /**
     * Sets the items from the given Collection and returns a
     * {@link ListDataView} that provides information and allows operations on
     * the items.
     *
     * @param items
     *            the items to display, not {@code null}
     * @return ListDataView providing access to the items
     */
    default V setItems(Collection<T> items) {
        return setItems(DataProvider.ofCollection(items));
    }

    /**
     * Sets the items of this component.
     *
     * @param items
     *            the items to display, not {@code null}
     * @return ListDataView providing access to the items
     */
    default V setItems(T... items) {
        return setItems(DataProvider.ofItems(items));
    }

    /**
     * Get the ListDataView for the component. Throws if the items are not
     * in-memory and should use another data view type like
     * {@code getLazyDataView()}.
     *
     * @return ListDataView providing access to the items
     * @throws IllegalStateException
     *             when list data view is not applicable and
     *             {@code getLazyDataView()} should be used instead
     */
    V getListDataView();
}
