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

import com.vaadin.flow.shared.Registration;

import java.util.Objects;

/**
 * Abstract data view implementation which takes care of processing
 * component data size change events.
 *
 * @param <T>
 *        data type
 */
public abstract class AbstractDataView<T> implements DataView<T> {

    protected DataController<T> dataController;

    /**
     * Creates a new instance of {@link AbstractDataView} subclass
     * and verifies the passed data controller is compatible with this
     * data view implementation.
     * <p>
     * Data controller reference is stored then internally and used for
     * data set manipulations.
     *
     * @param dataController
     *          data controller reference
     */
    public AbstractDataView(DataController<T> dataController) {
        Objects.requireNonNull(dataController, "DataController cannot be null");
        this.dataController = dataController;
        DataProvider<T, ?> dataProvider = dataController.getDataProvider();
        Objects.requireNonNull(dataProvider, "DataProvider cannot be null");
        verifyDataProviderType(dataProvider.getClass());
    }

    @Override
    public Registration addSizeChangeListener(SizeChangeListener listener) {
        Objects.requireNonNull(listener, "SizeChangeListener cannot be null");
        return dataController.addSizeChangeListener(listener);
    }

    /**
     * Returns supported {@link DataProvider} type for this {@link DataView}.
     *
     * @return supported data provider type
     */
    protected abstract Class<?> getSupportedDataProviderType();

    /**
     * Returns a base {@link DataView} class type for this data view.
     *
     * @return base data view type
     */
    protected abstract Class<?> getDataViewType();

    /**
     * Verifies an obtained {@link DataProvider} type is appropriate
     * for current Data View type.
     *
     * @param dataProviderType
     *              data provider type to be verified
     *
     * @throws IllegalStateException
     *              if data provider type is incompatible with data view type
     */
    protected final void verifyDataProviderType(Class<?> dataProviderType) {
        Class<?> supportedDataProviderType = getSupportedDataProviderType();
        Class<?> dataViewType = getDataViewType();
        if (!supportedDataProviderType.isAssignableFrom(dataProviderType)) {
            final String message = String
                    .format("%s only supports '%s' or it's subclasses, but was given a '%s'",
                            dataViewType.getSimpleName(), supportedDataProviderType.getSimpleName(),
                            dataProviderType.getSuperclass().getSimpleName());
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Obtains an appropriate {@link DataProvider} instance from {@link DataController}.
     * Throws a runtime exception otherwise, if the {@link DataProvider} instance is incompatible
     * with current implementation of {@link DataView}.
     *
     * @return data provider instance
     *
     * @throws IllegalStateException if data provider type is incompatible
     */
    protected abstract DataProvider<T, ?> getDataProvider();
}
