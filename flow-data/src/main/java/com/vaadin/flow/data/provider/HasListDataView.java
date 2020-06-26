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
import java.util.Collection;

/**
 * An interface for components that accept setting in-memory data sources and
 * returns a {@link ListDataView} that provides information and allows
 * operations on the data.
 *
 * @param <T>
 *            data type
 * @param <V>
 *            DataView type
 * @since
 */
public interface HasListDataView<T, V extends ListDataView<T, ?>>
        extends Serializable {
    /**
     * Sets a ListDataProvider for the component to use and returns a
     * {@link ListDataView} that provides information and allows operations on
     * the data.
     *
     * @param dataProvider
     *            ListDataProvider providing data to the component.
     * @return ListDataView providing access to the data
     */
    V setDataSource(ListDataProvider<T> dataProvider);

    /**
     * Sets the data from the given Collection and returns a
     * {@link ListDataView} that provides information and allows operations on
     * the data.
     *
     * @param items
     *            the data items to display, not {@code null}
     * @return ListDataView providing access to the data
     */
    default V setDataSource(Collection<T> items) {
        return setDataSource(DataProvider.ofCollection(items));
    }

    /**
     * Sets the data items of this listing.
     *
     * @param items
     *            the data items to display, not {@code null}
     * @return ListDataView providing access to the data
     */
    default V setDataSource(T... items) {
        return setDataSource(DataProvider.ofItems(items));
    }

    /**
     * Get the ListDataView for the component. Throws if the data is not
     * in-memory and should use another data view type like
     * {@code getLazyDataView()}.
     *
     * @return ListDataView providing access to the data
     * @throws IllegalStateException
     *             when list data view is not applicable and
     *             {@code getLazyDataView()} should be used instead
     */
    V getListDataView();
}
