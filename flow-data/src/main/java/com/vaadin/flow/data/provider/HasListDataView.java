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
