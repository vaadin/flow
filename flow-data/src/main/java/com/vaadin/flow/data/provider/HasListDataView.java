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
import java.util.stream.Stream;

/**
 * Interface that defines methods for setting in memory data.
 * This will return a {@link ListDataView}.
 *
 * @param <T>
 *         data type
 * @param <V>
 *         DataView type
 * @since
 */
public interface HasListDataView<T, V extends ListDataView<T>> extends
        Serializable {

    /**
     * Sets a ListDataProvider for the component to use.
     *
     * @param dataProvider
     *         ListDataProvider providing data tot he component.
     * @return ListDataView instance
     */
    V setDataProvider(ListDataProvider<T> dataProvider);

    // NOTE: this is not using setItems so that we don't collide with HasItems::setItems

    /**
     * Sets the data items from the given Collection.
     *
     * @param items
     *         the data items to display, not {@code null}
     * @return ListDataView instance
     */
    V setDataProvider(Collection<T> items);

    /**
     * Sets the data items from the given Stream.
     *
     * @param items
     *         the data items to display, not {@code null}
     * @return ListDataView instance
     */
    V setDataProvider(Stream<T> items);

    /**
     * Sets the data items of this listing.
     *
     * @param items
     *         the data items to display, not {@code null}
     * @return ListDataView instance
     */
    V setDataProvider(T... items);

    /**
     * Get the ListDataView for the component. Throws if the data is not
     * in-memory and should use another data view type.
     *
     * @return ListDataView instance
     * @throws IllegalStateException
     *         when list data view is not applicable
     */
    V getListDataView();
}