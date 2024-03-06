/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Abstract hierarchical data provider implementation which takes care of item
 * refreshes and associated events.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            data type
 * @param <F>
 *            filter type
 * @since 1.2
 */
public abstract class AbstractHierarchicalDataProvider<T, F> extends
        AbstractDataProvider<T, F> implements HierarchicalDataProvider<T, F> {

    @Override
    public <Q, C> HierarchicalConfigurableFilterDataProvider<T, Q, C> withConfigurableFilter(
            SerializableBiFunction<Q, C, F> filterCombiner) {
        return HierarchicalDataProvider.super.withConfigurableFilter(
                filterCombiner);
    }

    @Override
    public <C> HierarchicalDataProvider<T, C> withConvertedFilter(
            SerializableFunction<C, F> filterConverter) {
        return HierarchicalDataProvider.super.withConvertedFilter(
                filterConverter);
    }

    @Override
    public HierarchicalConfigurableFilterDataProvider<T, Void, F> withConfigurableFilter() {
        return (HierarchicalConfigurableFilterDataProvider<T, Void, F>) super.withConfigurableFilter();
    }
}
