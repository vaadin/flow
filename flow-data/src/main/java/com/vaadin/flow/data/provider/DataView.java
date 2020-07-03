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
import java.util.stream.Stream;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

/**
 * Base view interface for getting information on current data set of a
 * Component.
 *
 * @param <T>
 *            data type
 * @since
 */
public interface DataView<T> extends Serializable {

    /**
     * Get the full data available to the component. Data is filtered and sorted
     * the same way as in the component.
     *
     * @return filtered and sorted data set
     */
    Stream<T> getItems();

    /**
     * Add an item count change listener that is fired when the item
     * count changes. This can happen for instance when filtering the data set.
     * <p>
     * Item count change listener is bound to the component and will be
     * retained even if the data changes by setting of a new items or
     * {@link DataProvider} to component.
     *
     * @param listener
     *            item count change listener to register
     * @return registration for removing the listener
     */
    Registration addItemCountChangeListener(
            ComponentEventListener<ItemCountChangeEvent<?>> listener);

    /**
     * Sets an identifier provider, which returns an identifier for the given
     * item. The identifier is used for comparing the equality of items. Usage
     * example: {@code dataView.setIdentifiedProvider(Item::getId);}.
     *
     * @param identifierProvider
     *            function that returns the non-null identifier for a given item
     */
    void setIdentifierProvider(IdentifierProvider<T> identifierProvider);
}
