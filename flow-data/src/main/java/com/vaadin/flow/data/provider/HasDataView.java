/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * An interface for components that get items from the generic data provider
 * types {@link DataProvider} and {@link InMemoryDataProvider}. The methods
 * return a {@link DataView} which has the generic API for getting information
 * on the items.
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @param <V>
 *            DataView type
 */
public interface HasDataView<T, F, V extends DataView<T>> extends Serializable {

    /**
     * Set a generic data provider for the component to use and returns the base
     * {@link DataView} that provides API to get information on the items.
     * <p>
     * This method should be used only when the data provider type is not either
     * {@link ListDataProvider} or {@link BackEndDataProvider}.
     *
     * @param dataProvider
     *            DataProvider instance to use, not <code>null</code>
     * @return DataView providing information on the data
     */
    V setItems(DataProvider<T, F> dataProvider);

    /**
     * Sets an in-memory data provider for the component to use
     * <p>
     * Note! Using a {@link ListDataProvider} instead of a
     * {@link InMemoryDataProvider} is recommended to get access to
     * {@link ListDataView} API by using
     * {@link HasListDataView#setItems(ListDataProvider)}.
     *
     * @param dataProvider
     *            InMemoryDataProvider to use, not <code>null</code>
     * @return DataView providing information on the data
     */
    V setItems(InMemoryDataProvider<T> dataProvider);

    /**
     * Get the DataView for the component.
     * <p>
     * The returned DataView only contains a minimal common API. Use of
     * {@link HasListDataView#getListDataView} or
     * {@link HasLazyDataView#getLazyDataView} should be used for more targeted
     * helper features
     *
     * @return DataView instance
     */
    V getGenericDataView();

    /**
     * Binds a signal containing a list of item signals to this component,
     * enabling fine-grained reactive updates. This method establishes a
     * reactive binding where:
     * <ul>
     * <li>Changes to the outer signal (list structure changes such as adding or
     * removing items) trigger {@link DataView#refreshAll()}</li>
     * <li>Changes to any inner signal (individual item value changes) trigger
     * {@link DataView#refreshItem(Object)} for that specific item</li>
     * </ul>
     * <p>
     * The binding is automatically managed based on the component's lifecycle.
     * When the component is attached, the effects are activated; when detached,
     * they are deactivated.
     * <p>
     * Example usage:
     *
     * <pre>
     * ListSignal&lt;String&gt; items = new ListSignal&lt;&gt;();
     * items.insertLast("Item 1");
     * items.insertLast("Item 2");
     *
     * component.bindItems(items);
     *
     * // Structural change - triggers refreshAll()
     * items.insertLast("Item 3");
     *
     * // Item value change - triggers refreshItem()
     * items.peek().get(0).set("Updated Item 1");
     * </pre>
     *
     * @param itemsSignal
     *            the signal containing a list of item signals (e.g.,
     *            ListSignal), not {@code null}
     * @return the DataView providing access to the items
     * @throws IllegalArgumentException
     *             if this is not a Component instance
     * @throws BindingActiveException
     *             if there is already an active items binding
     */
    default V bindItems(
            Signal<? extends List<? extends Signal<T>>> itemsSignal) {
        Objects.requireNonNull(itemsSignal, "Items signal cannot be null");

        if (!(this instanceof Component component)) {
            throw new IllegalArgumentException(
                    "bindItems can only be used with Component instances");
        }

        // Check if there's already an active binding
        SignalBindingFeature bindingFeature = component.getElement().getNode()
                .getFeature(SignalBindingFeature.class);
        if (bindingFeature.hasBinding(SignalBindingFeature.ITEMS)) {
            throw new BindingActiveException(
                    "Cannot bind items: a binding is already active");
        }

        // Create a mutable backing list for the data provider
        List<T> backingList = new ArrayList<>(
                Objects.requireNonNull(itemsSignal.peek()).size());

        // Create and set the data provider
        V dataView = setItems(DataProvider.ofCollection(backingList));

        // List to store inner effect registrations
        List<Registration> innerEffectRegistrations = new ArrayList<>();

        // Outer effect: tracks changes to the list structure
        Registration outerEffect = Signal.effect(component, () -> {
            List<? extends Signal<T>> currentSignals = Objects
                    .requireNonNull(itemsSignal.get());

            // Dispose old inner effects
            innerEffectRegistrations.forEach(Registration::remove);
            innerEffectRegistrations.clear();

            // Extract current values from all inner signals
            backingList.clear();
            for (Signal<T> signal : currentSignals) {
                T value = signal.peek();
                backingList.add(value);
            }

            // Refresh all data
            dataView.refreshAll();

            // Set up new inner effects for each signal
            for (int i = 0; i < currentSignals.size(); i++) {
                Signal<T> itemSignal = currentSignals.get(i);
                int index = i;

                // Track whether this is the first run of the inner effect
                AtomicBoolean isFirstRun = new AtomicBoolean(true);

                Registration innerEffect = Signal.effect(component, () -> {
                    T newValue = itemSignal.get();

                    // Skip refreshItem on the first run since refreshAll was
                    // just called
                    if (!isFirstRun.get()) {
                        backingList.set(index, newValue);
                        dataView.refreshItem(newValue);
                    }
                    isFirstRun.set(false);
                });

                innerEffectRegistrations.add(innerEffect);
            }
        });

        // Store the binding in SignalBindingFeature to track active binding
        bindingFeature.setBinding(SignalBindingFeature.ITEMS, outerEffect,
                itemsSignal);

        return dataView;
    }
}
