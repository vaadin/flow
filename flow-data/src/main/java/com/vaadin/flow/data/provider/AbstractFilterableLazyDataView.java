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

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;

/**
 * Abstract filterable lazy data view implementation which handles the
 * interaction with a data communicator and filtering set up.
 *
 * @param <T>
 *            the type of data
 * @param <F>
 *            the type of filter
 */
public abstract class AbstractFilterableLazyDataView<T, F> extends
        AbstractLazyDataView<T> implements FilterableLazyDataView<T, F> {

    private SerializableConsumer<Object> filterConsumer;

    private SerializableSupplier<F> filterSupplier;

    // Merges a component's old filter and a new configured filter
    private FilterCombiner<F, ?> filterCombiner;

    /**
     * Creates a new instance, verifies the passed data provider is compatible
     * with this data view implementation, and sets a callbacks for handling the
     * items filtering.
     *
     * @param dataCommunicator
     *            the data communicator of the component, not <code>null</code>
     * @param component
     *            the component, not <code>null</code>
     * @param filterConsumer
     *            callback for changing the filter value in component, not
     *            <code>null</code>
     * @param filterSupplier
     *            supplier for component's internal filter value, not
     *            <code>null</code>
     */
    public AbstractFilterableLazyDataView(DataCommunicator<T> dataCommunicator,
            Component component, SerializableConsumer<Object> filterConsumer,
            SerializableSupplier<F> filterSupplier,
                                          FilterCombiner<F, ?> filterCombiner) {
        super(dataCommunicator, component);
        Objects.requireNonNull(filterConsumer,
                "Filter consumer cannot be null");
        Objects.requireNonNull(filterSupplier,
                "Filter supplier cannot be null");
        this.filterConsumer = filterConsumer;
        this.filterSupplier = filterSupplier;
        this.filterCombiner = filterCombiner;
    }

    @Override
    public void setFilter(Object filter) {
        Object combinedFilter = filterCombiner.apply(filterSupplier.get(), filter);
        filterConsumer.accept(combinedFilter);
    }

    @Override
    public void setItemCountCallbackWithFilter(
            CallbackDataProvider.CountCallback<T, F> callback) {
        getDataCommunicator().setCountCallback(callback);
    }
}
