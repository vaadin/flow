/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract data view implementation which handles parts that apply for any type
 * of data.
 *
 * @param <T>
 *            data type
 */
public abstract class AbstractDataView<T> implements DataView<T> {

    protected static final String NULL_ITEM_ERROR_MESSAGE = "Item cannot be null";
    protected static final String NULL_IDENTIFIER_ERROR_MESSAGE = "Identity provider should not return null";

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
        final Class<?> dataProviderType = dataProviderSupplier.get().getClass();
        /*
         * Skip verification if the verified data provider has not been
         * initialized yet.
         *
         * This mainly refers to the following cases: 1. Component uses data
         * communicator which initialises data provider lazily and meanwhile has
         * a default empty one. Good example is a ComboBox. 2. Developer wants
         * to set the ItemCountChangeListener before the data provider has been
         * set.
         *
         * NOTE: In-memory data view API is supported without explicitly setting
         * the data provider.
         */
        if (isDataProviderInitialized(dataProviderType)) {
            verifyDataProviderType(dataProviderSupplier.get());
        }
    }

    @Override
    public Registration addItemCountChangeListener(
            ComponentEventListener<ItemCountChangeEvent<?>> listener) {
        Objects.requireNonNull(listener,
                "ItemCountChangeListener cannot be null");
        return ComponentUtil.addListener(component, ItemCountChangeEvent.class,
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
        Class<?> supportedDataProviderType = getSupportedDataProviderType();
        if (!supportedDataProviderType.isAssignableFrom(dataProviderType)) {
            final String message = String.format(
                    "%s only supports '%s' or it's subclasses, but was given a '%s'."
                            + "%nUse either 'getLazyDataView()', 'getListDataView()'"
                            + " or 'getGenericDataView()' according to the "
                            + "used data type.",
                    this.getClass().getSimpleName(),
                    supportedDataProviderType.getSimpleName(),
                    dataProviderType.getSuperclass().getSimpleName());
            throw new IllegalStateException(message);
        }
    }

    /**
     * Verifies an obtained {@link DataProvider} type is appropriate for current
     * Data View type. If the data provider is a wrapper, then the wrapped data
     * provider is verified too.
     *
     * @param dataProvider
     *            data provider to be verified
     * @throws IllegalStateException
     *             if data provider type is incompatible with data view type
     */
    protected final void verifyDataProviderType(
            DataProvider<T, ?> dataProvider) {
        try {
            verifyDataProviderType(dataProvider.getClass());
        } catch (IllegalStateException e) {
            if (DataProviderWrapper.class
                    .isAssignableFrom(dataProvider.getClass())) {
                verifyDataProviderType(
                        ((DataProviderWrapper<T, ?, ?>) dataProvider)
                                .getWrappedDataProvider());
            } else {
                throw e;
            }
        }
    }

    @Override
    public Optional<Integer> getItemIndex(T item) {
        int index = getItemIndex(item, getItems());
        return index >= 0 ? Optional.of(index) : Optional.empty();
    }

    @Override
    public Stream<T> getItems() {
        return dataProviderSupplier.get().fetch(new Query<>());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Stream<T> getItems(final int offset, final int limit) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Offset must be non-negative");
        }
        if (limit < 0) {
            throw new IndexOutOfBoundsException("Limit must be non-negative");
        }
        if (limit == 0) {
            return Stream.empty();
        }
        return dataProviderSupplier.get().fetch(
                new Query(offset, limit, Collections.emptyList(), null, null));
    }

    @Override
    public void refreshItem(T item) {
        Objects.requireNonNull(item, NULL_ITEM_ERROR_MESSAGE);
        dataProviderSupplier.get().refreshItem(item);
    }

    @Override
    public void refreshAll() {
        dataProviderSupplier.get().refreshAll();
    }

    @Override
    public void setIdentifierProvider(
            IdentifierProvider<T> identifierProvider) {
        Objects.requireNonNull(identifierProvider,
                "Item identity provider cannot be null");
        ComponentUtil.setData(component, IdentifierProvider.class,
                identifierProvider);
        ComponentUtil.fireEvent(component, new IdentifierProviderChangeEvent<>(
                component, identifierProvider));
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

    /**
     * Add an identifier provider change listener that is fired when a custom
     * identifier provider is set with
     * {@link #setIdentifierProvider(IdentifierProvider)}.
     * <p>
     * Can be used by components to get notified that a new identifier provider
     * has been set through the data view.
     *
     * @param listener
     *            identifier provider change listener to register
     * @return registration for removing the listener
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Registration addIdentifierProviderChangeListener(
            ComponentEventListener<IdentifierProviderChangeEvent<T, ?>> listener) {
        Objects.requireNonNull(listener,
                "IdentifierProviderChangeListener cannot be null");
        return ComponentUtil.addListener(component,
                IdentifierProviderChangeEvent.class,
                (ComponentEventListener) listener);
    }

    protected boolean equals(T item, T compareTo) {
        return Objects.equals(
                Objects.requireNonNull(getIdentifierProvider().apply(item),
                        NULL_IDENTIFIER_ERROR_MESSAGE),
                Objects.requireNonNull(getIdentifierProvider().apply(compareTo),
                        NULL_IDENTIFIER_ERROR_MESSAGE));
    }

    /**
     * Gets item index from the given stream.
     *
     * @param item
     *            the item to get index for
     * @param stream
     *            the stream to get index from
     * @return the index of the item in the stream, or -1 if not found
     */
    protected int getItemIndex(T item, Stream<T> stream) {
        Objects.requireNonNull(item, NULL_ITEM_ERROR_MESSAGE);
        AtomicInteger index = new AtomicInteger(-1);
        //@formatter:off
        if (!stream.peek(nextItem -> index.incrementAndGet())
                .filter(nextItem -> equals(item, nextItem))
                .findFirst().isPresent()) {
            return -1;
        }
        //@formatter:on
        return index.get();
    }

    private boolean isDataProviderInitialized(Class<?> dataProviderType) {
        return !DataCommunicator.EmptyDataProvider.class
                .isAssignableFrom(dataProviderType);
    }
}
