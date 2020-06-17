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

/**
 * Interface that defines methods for setting data.
 * This will return a {@link DataView}.
 *
 * @param <T>
 *         data type
 * @param <V>
 *         DataView type
 * @since
 */
public interface HasDataView<T, V extends DataView<T>> extends Serializable {

    /**
     * Set a generic data provider for the component to use.
     * <p>
     * It is suggested to use a more specific target from {@link
     * HasListDataView} or {@link HasLazyDataView}
     *
     * @param dataProvider
     *         DataProvider instance to use
     * @return DataView instance
     */
    V setDataSource(DataProvider<T, ?> dataProvider);

    /**
     * Sets an InMemory data provider for the component to use.
     * <p>
     * Note! Using a {@link ListDataProvider} instead of a {@link
     * InMemoryDataProvider} is recommended to get access to {@link
     * ListDataView} API by using {@link HasListDataView#setDataSource(ListDataProvider)}.
     *
     * @param dataProvider
     *         InMemoryDataProvider to use
     * @return DataView instance
     */
    default V setDataSource(InMemoryDataProvider<T> dataProvider) {
        return setDataSource((DataProvider<T, ?>) dataProvider);
    }

    /**
     * Get the DataView for the component.
     * <p>
     * The returned DataView only contains a minimal common API.
     * Use of {@link HasListDataView#getListDataView} or {@link
     * HasLazyDataView#getLazyDataView} should be used for more
     * targeted helper features
     *
     * @return DataView instance
     */
    V getDataView();
}
