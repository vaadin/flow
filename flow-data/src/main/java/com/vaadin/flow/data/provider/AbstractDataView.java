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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.Registration;

import java.util.Objects;

/**
 * Abstract data view implementation which takes care of processing
 * component data size change events.
 *
 * @param <T>
 *        data type
 *
 * @param <C>
 *        component type
 */
public abstract class AbstractDataView<T, C extends Component> implements DataView<T> {

    protected DataController<T> dataController;

    public AbstractDataView(DataController<T> dataController) {
        Objects.requireNonNull(dataController, "DataController cannot be null");
        this.dataController = dataController;
        validateDataProvider(dataController.getDataProvider());
    }

    @Override
    public Registration addSizeChangeListener(SizeChangeListener listener) {
        Objects.requireNonNull(listener, "SizeChangeListener cannot be null");
        return dataController.addSizeChangeListener(listener);
    }

    /**
     * Validates an obtained {@link DataProvider} instance type is appropriate for current Data View type.
     *
     * @param dataProvider
     *              data provider instance to be validated
     *
     * @throws IllegalStateException
     *              if data provider type is incompatible with data view type
     */
    protected abstract void validateDataProvider(final DataProvider<T, ?> dataProvider);

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
