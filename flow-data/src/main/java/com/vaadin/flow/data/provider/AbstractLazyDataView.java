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
        verifyDataProviderType(dataCommunicator.getDataProvider().getClass());
        return dataCommunicator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> getItems() {
        DataCommunicator<T> verifiedDataCommunicator = getDataCommunicator();
        if (verifiedDataCommunicator.isDefinedSize()) {
            return verifiedDataCommunicator.getDataProvider()
                    .fetch(this.dataCommunicator.buildQuery(0,
                            this.dataCommunicator.getDataSize()));
        } else {
            return verifiedDataCommunicator.getDataProvider().fetch(
                    this.dataCommunicator.buildQuery(0, Integer.MAX_VALUE));
        }
    }

    /**
     * Gets the size of the data source. With unknown size
     * {@link #setRowCountUnknown()} this may be an estimate. <em>NOTE: as the
     * size might change at any point - add a listener with the
     * {@link #addSizeChangeListener(ComponentEventListener)} method to get
     * notified when the data size has changed. Calling this method will also
     * trigger a backend call when using either a
     * {@link #setRowCountFromDataProvider()} or
     * {@link #setRowCountCallback(CallbackDataProvider.CountCallback)} and the
     * size is not yet fetched.</em>
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
    public void setRowCountCallback(
            CallbackDataProvider.CountCallback<T, Void> callback) {
        getDataCommunicator().setCountCallback(callback);
    }

    @Override
    public void setRowCountEstimate(int rowCountEstimate) {
        getDataCommunicator().setRowCountEstimate(rowCountEstimate);
    }

    @Override
    public int getRowCountEstimate() {
        return getDataCommunicator().getRowCountEstimate();
    }

    @Override
    public void setRowCountEstimateStep(int rowCountEstimateStep) {
        getDataCommunicator().setRowCountEstimateStep(rowCountEstimateStep);
    }

    @Override
    public int getRowCountEstimateStep() {
        return getDataCommunicator().getRowCountEstimateStep();
    }

    @Override
    public void setRowCountFromDataProvider() {
        getDataCommunicator().setDefinedSize(true);
    }

    @Override
    public void setRowCountUnknown() {
        getDataCommunicator().setDefinedSize(false);
    }

    @Override
    public boolean contains(T item) {
        return getDataCommunicator().isItemActive(item);
    }

}
