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

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;

/**
 * Abstract lazy data view implementation which handles the interaction with a
 * data communicator.
 * 
 * @param <T>
 *            the type of data
 */
public abstract class AbstractLazyDataView<T> extends AbstractDataView<T>
        implements LazyDataView<T> {

    private final DataCommunicator<T> dataCommunicator;

    /**
     * Creates a new instance and verifies the passed data provider is
     * compatible with this data view implementation.
     *
     * @param dataCommunicator
     *            the data communicator of the component
     * @param component
     *            the component
     */
    public AbstractLazyDataView(DataCommunicator<T> dataCommunicator,
            Component component) {
        super(dataCommunicator::getDataProvider, component);
        this.dataCommunicator = dataCommunicator;
    }

    /**
     * Returns the data communicator for the component and checks that the data
     * provider is of the correct type.
     * 
     * @return the data communicator
     */
    protected DataCommunicator<T> getDataCommunicator() {
        // verify that the data provider hasn't been changed to an incompatible
        // type
        if (dataCommunicator.getDataProvider().isInMemory()) {
            throw new IllegalStateException(String.format(
                    "LazyDataView cannot be used for component %s with an "
                            + "in-memory data provider (type was %s)."
                            + "Use a lazy data provider instead or"
                            + " for accessing data view for in-memory data use"
                            + " getListDataView().",
                    component.getClass().getSimpleName(), dataCommunicator
                            .getDataProvider().getClass().getSimpleName()));
        }
        return dataCommunicator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> getItems() {
        if (isDefinedSize()) {
            return getDataCommunicator().getDataProvider()
                    .fetch(dataCommunicator.buildQuery(0,
                            dataCommunicator.getDataSize()));
        } else {
            return getDataCommunicator().getDataProvider()
                    .fetch(dataCommunicator.buildQuery(0, Integer.MAX_VALUE));
        }
    }

    /**
     * Gets the known size of the data. With undefined size
     * {@link #withUndefinedSize()} this may be an estimate. <em>NOTE: usage of
     * this method is not recommended as the size might change at any point -
     * add a listener with the
     * {@link #addSizeChangeListener(ComponentEventListener)} method to get
     * notified when the data size has changed. Calling this method will also
     * trigger a backend call when using {@link #withDefinedSize()} and the size
     * is not known.</em>
     * <p>
     * If size is not yet known, like during the initial roundtrip, {@code 0} is
     * returned as the size is determined during the "before client
     * response"-phase.
     * 
     * @return the size of the data or the currently used estimated size
     */
    @Override
    public int getSize() {
        return getDataCommunicator().getDataSize();
    }

    @Override
    protected Class<?> getSupportedDataProviderType() {
        return BackEndDataProvider.class;
    }

    @Override
    public void withDefinedSize(
            CallbackDataProvider.CountCallback<T, Void> callback) {
        getDataCommunicator().setSizeCallback(callback);
    }

    @Override
    public void withUndefinedSize(int initialSizeEstimate) {
        getDataCommunicator().setInitialSizeEstimate(initialSizeEstimate);
    }

    @Override
    public void withUndefinedSize(SizeEstimateCallback<T, Void> callback) {
        getDataCommunicator().setSizeEstimateCallback(callback);
    }

    @Override
    public void withDefinedSize() {
        getDataCommunicator().setDefinedSize(true);
    }

    @Override
    public void withUndefinedSize() {
        getDataCommunicator().setDefinedSize(false);
    }

    @Override
    public boolean isDefinedSize() {
        return getDataCommunicator().isDefinedSize();
    }

    @Override
    public boolean contains(T item) {
        return getDataCommunicator().isItemActive(item);
    }

}
