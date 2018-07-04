/*
 * Copyright 2000-2018 Vaadin Ltd.
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
