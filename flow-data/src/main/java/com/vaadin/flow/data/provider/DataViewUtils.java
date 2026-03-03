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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * Internal utility class used by data view implementations and components to
 * simplify the filtering and sorting handling, but not limited to it.
 *
 * @author Vaadin Ltd
 */
public final class DataViewUtils {

    private static final String COMPONENT_IN_MEMORY_FILTER_KEY = "component-in-memory-filter-key";
    private static final String COMPONENT_IN_MEMORY_SORTING_KEY = "component-in-memory-sorting-key";

    private DataViewUtils() {
        // avoid instantiating utility class
    }

    /**
     * Gets the in-memory filter of a given component instance.
     *
     * @param component
     *            component instance the filter is bound to
     * @param <T>
     *            item type
     * @return optional component's in-memory filter.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<SerializablePredicate<T>> getComponentFilter(
            Component component) {
        return Optional.ofNullable((SerializablePredicate<T>) ComponentUtil
                .getData(component, COMPONENT_IN_MEMORY_FILTER_KEY));
    }

    /**
     * Gets the in-memory sort comparator of a given component instance.
     *
     * @param component
     *            component instance the sort comparator is bound to
     * @param <T>
     *            item type
     * @return optional component's in-memory sort comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<SerializableComparator<T>> getComponentSortComparator(
            Component component) {
        return Optional.ofNullable((SerializableComparator<T>) ComponentUtil
                .getData(component, COMPONENT_IN_MEMORY_SORTING_KEY));
    }

    /**
     * Sets the in-memory filter to a given component instance. The filter
     * replaces any filter that has been set or added previously. {@code null}
     * will clear all filters.
     *
     * @param component
     *            component instance the filter is bound to
     * @param filter
     *            component's in-memory filter to be set, or <code>null</code>
     *            to clear any previously set filters
     * @param <T>
     *            items type
     */
    public static <T> void setComponentFilter(Component component,
            SerializablePredicate<T> filter) {
        ComponentUtil.setData(component, COMPONENT_IN_MEMORY_FILTER_KEY,
                filter);
    }

    /**
     * Sets the in-memory sort comparator to a given component instance. The
     * sort comparator replaces any sort comparator that has been set or added
     * previously. {@code null} will clear all sort comparators.
     *
     * @param component
     *            component instance the sort comparator is bound to
     * @param sortComparator
     *            component's in-memory sort comparator to be set, or
     *            <code>null</code> to clear any previously set sort comparators
     * @param <T>
     *            items type
     */
    public static <T> void setComponentSortComparator(Component component,
            SerializableComparator<T> sortComparator) {
        ComponentUtil.setData(component, COMPONENT_IN_MEMORY_SORTING_KEY,
                sortComparator);
    }

    /**
     * Removes the in-memory filter and sort comparator from a given component
     * instance.
     *
     * @param component
     *            component instance the filter and sort comparator are removed
     *            from
     */
    public static void removeComponentFilterAndSortComparator(
            Component component) {
        setComponentFilter(component, null);
        setComponentSortComparator(component, null);
    }

    /**
     * Generates a data query with component's in-memory filter and sort
     * comparator.
     *
     * @param component
     *            component instance the filter and sort comparator are bound to
     * @return query instance
     */
    @SuppressWarnings({ "rawtypes" })
    public static Query getQuery(Component component) {
        return getQuery(component, true);
    }

    /**
     * Generates a data query with component's in-memory filter and sort
     * comparator, which is optionally included if {@code withSorting} is set to
     * {@code true}.
     *
     * @param component
     *            component instance the filter and sort comparator are bound to
     * @param withSorting
     *            if {@code true}, the component's sort comparator will be
     *            included in the query.
     * @return query instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Query getQuery(Component component, boolean withSorting) {
        final Optional<SerializablePredicate<Object>> filter = DataViewUtils
                .getComponentFilter(component);

        final Optional<SerializableComparator<Object>> sorting = withSorting
                ? DataViewUtils.getComponentSortComparator(component)
                : Optional.empty();

        return new Query(0, Integer.MAX_VALUE, null, sorting.orElse(null),
                filter.orElse(null));
    }

    /**
     * Binds a signal containing a list of item signals to a component, enabling
     * fine-grained reactive updates. This is a helper method used by
     * {@link HasDataView#bindItems(Signal)}.
     *
     * @param hasDataView
     *            the component that implements HasDataView, not {@code null}
     * @param itemsSignal
     *            the signal containing a list of item signals, not {@code null}
     * @param dataProviderSetter
     *            function that sets the data provider and returns a DataView,
     *            not {@code null}
     * @param <T>
     *            item type
     * @param <V>
     *            DataView type
     * @return the DataView providing access to the items
     * @throws IllegalArgumentException
     *             if hasDataView is not a Component instance
     * @throws BindingActiveException
     *             if there is already an active items binding
     */
    public static <T, V extends DataView<T>> V bindItems(
            HasDataView<T, ?, V> hasDataView,
            Signal<? extends List<? extends Signal<T>>> itemsSignal,
            SerializableFunction<List<T>, V> dataProviderSetter) {
        Objects.requireNonNull(hasDataView, "HasDataView cannot be null");
        Objects.requireNonNull(itemsSignal, "Items signal cannot be null");
        Objects.requireNonNull(dataProviderSetter,
                "Data provider setter cannot be null");

        if (!(hasDataView instanceof Component component)) {
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

        // Create and set the data provider using the provided setter
        V dataView = dataProviderSetter.apply(backingList);

        // List to store inner effect registrations
        List<Registration> innerEffectRegistrations = new ArrayList<>();

        // Outer effect: tracks changes to the list structure
        Registration outerEffect = Signal.effect(component, () -> {
            List<? extends Signal<T>> currentSignals = Objects
                    .requireNonNull(itemsSignal.get());

            // Dispose old inner effects
            innerEffectRegistrations.forEach(Registration::remove);
            innerEffectRegistrations.clear();

            // Update the backing list with current signal values
            updateBackingList(currentSignals, backingList);

            // Refresh all data
            dataView.refreshAll();

            // Set up new inner effects for each signal
            createInnerEffects(component, currentSignals, backingList, dataView,
                    innerEffectRegistrations);
        });

        // Store the binding in SignalBindingFeature to track active binding
        bindingFeature.setBinding(SignalBindingFeature.ITEMS, outerEffect,
                itemsSignal);

        return dataView;
    }

    /**
     * Updates the backing list with current values from the item signals.
     *
     * @param currentSignals
     *            list of signals containing item values
     * @param backingList
     *            the backing list to update
     * @param <T>
     *            item type
     */
    private static <T> void updateBackingList(
            List<? extends Signal<T>> currentSignals, List<T> backingList) {
        backingList.clear();
        for (Signal<T> signal : currentSignals) {
            T value = signal.peek();
            backingList.add(value);
        }
    }

    /**
     * Creates inner effects for each item signal to track individual item
     * changes.
     *
     * @param component
     *            the component to bind effects to
     * @param currentSignals
     *            list of signals containing item values
     * @param backingList
     *            the backing list to update
     * @param dataView
     *            the data view to refresh
     * @param innerEffectRegistrations
     *            list to store the effect registrations
     * @param <T>
     *            item type
     * @param <V>
     *            DataView type
     */
    private static <T, V extends DataView<T>> void createInnerEffects(
            Component component, List<? extends Signal<T>> currentSignals,
            List<T> backingList, V dataView,
            List<Registration> innerEffectRegistrations) {
        for (int i = 0; i < currentSignals.size(); i++) {
            Signal<T> itemSignal = currentSignals.get(i);

            Registration innerEffect = createItemEffect(component, itemSignal,
                    i, backingList, dataView);

            innerEffectRegistrations.add(innerEffect);
        }
    }

    /**
     * Creates an effect for a single item signal that updates the backing list
     * and refreshes the item when it changes. Skips the first execution to
     * avoid redundant refresh after refreshAll.
     *
     * @param component
     *            the component to bind the effect to
     * @param itemSignal
     *            the signal for a single item
     * @param index
     *            the index of the item in the backing list
     * @param backingList
     *            the backing list to update
     * @param dataView
     *            the data view to refresh
     * @param <T>
     *            item type
     * @param <V>
     *            DataView type
     * @return the registration for the effect
     */
    private static <T, V extends DataView<T>> Registration createItemEffect(
            Component component, Signal<T> itemSignal, int index,
            List<T> backingList, V dataView) {
        // Track whether this is the first run of the effect
        AtomicBoolean isFirstRun = new AtomicBoolean(true);
        return Signal.effect(component, () -> {
            T newValue = itemSignal.get();

            // Skip refreshItem on the first run since refreshAll was just
            // called
            if (!isFirstRun.get()) {
                backingList.set(index, newValue);
                dataView.refreshItem(newValue);
            }
            isFirstRun.set(false);
        });
    }
}
