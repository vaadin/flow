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
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract data view implementation which handles parts that apply for any type
 * of data source.
 *
 * @param <T>
 *            data type
 */
public abstract class AbstractDataView<T> implements DataView<T> {

    protected SerializableSupplier<? extends DataProvider<T, ?>> dataProviderSupplier;
    protected Component component;

    /**
     * Creates a new instance of {@link AbstractDataView} subclass and verifies
     * the passed data provider is compatible with this data view
     * implementation.
     *
     * @param dataProviderSupplier
     *            supplier from which the DataProvider can be gotten
     * @param component
     *            the component that the dataView is bound to
     */
    public AbstractDataView(
            SerializableSupplier<? extends DataProvider<T, ?>> dataProviderSupplier,
            Component component) {
        Objects.requireNonNull(dataProviderSupplier,
                "DataProvider supplier cannot be null");
        this.dataProviderSupplier = dataProviderSupplier;
        this.component = component;
        verifyDataProviderType(dataProviderSupplier.get().getClass());
    }

    @Override
    public Registration addSizeChangeListener(
            ComponentEventListener<SizeChangeEvent<?>> listener) {
        Objects.requireNonNull(listener, "SizeChangeListener cannot be null");
        return ComponentUtil.addListener(component, SizeChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Returns supported {@link DataProvider} type for this {@link DataView}.
     *
     * @return supported data provider type
     */
    protected abstract Class<?> getSupportedDataProviderType();

    /**
     * Verifies an obtained {@link DataProvider} type is appropriate for current
     * Data View type.
     *
     * @param dataProviderType
     *            data provider type to be verified
     * @throws IllegalStateException
     *             if data provider type is incompatible with data view type
     */
    protected final void verifyDataProviderType(Class<?> dataProviderType) {
        // TODO https://github.com/vaadin/flow/issues/8583
        Class<?> supportedDataProviderType = getSupportedDataProviderType();
        if (!supportedDataProviderType.isAssignableFrom(dataProviderType)) {
            final String message = String.format(
                    "%s only supports '%s' or it's subclasses, but was given a '%s'."
                            + "%nUse either 'getLazyDataView()', 'getListDataView()'"
                            + " or getDataView() according to the used data type.",
                    this.getClass().getSimpleName(),
                    supportedDataProviderType.getSimpleName(),
                    dataProviderType.getSuperclass().getSimpleName());
            throw new IllegalStateException(message);
        }
    }

    @Override
    public Stream<T> getItems() {
        return dataProviderSupplier.get().fetch(new Query<>());
    }

    @Override
    public int getSize() {
        return dataProviderSupplier.get().size(new Query<>());
    }

    @Override
    public void setIdentifierProvider(
            IdentifierProvider<T> identifierProvider) {
        Objects.requireNonNull(identifierProvider,
                "Item identity provider cannot be null");
        ComponentUtil.setData(component, IdentifierProvider.class,
                identifierProvider);
    }

    @SuppressWarnings("unchecked")
    protected IdentifierProvider<T> getIdentifierProvider() {
        IdentifierProvider<T> identifierProviderObject = (IdentifierProvider<T>) ComponentUtil
                .getData(component, IdentifierProvider.class);

        if (identifierProviderObject == null) {
            DataProvider<T, ?> dataProvider = dataProviderSupplier.get();
            if (dataProvider != null) {
                return dataProvider::getId;
            } else {
                return IdentifierProvider.identity();
            }
        } else {
            return identifierProviderObject;
        }
    }
}
