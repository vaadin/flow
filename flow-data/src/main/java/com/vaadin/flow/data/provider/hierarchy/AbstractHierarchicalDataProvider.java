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
 * @since 1.1
 */
public abstract class AbstractHierarchicalDataProvider<T, F> extends
        AbstractDataProvider<T, F> implements HierarchicalDataProvider<T, F> {

    /**
     * {@inheritDoc}
     * <p>
     * A {@code null} item represents the virtual root of the hierarchy (the
     * parent of root-level items, consistent with APIs such as
     * {@link TreeData#addItem(Object, Object)}). Refreshing the virtual root
     * refreshes root-level items only; it is <em>not</em> equivalent to
     * {@link #refreshAll()}. Use {@link #refreshItem(Object, boolean)
     * refreshItem(null, true)} to also re-fetch the root-level children list,
     * or {@link #refreshAll()} to rebuild the entire hierarchy (which may reset
     * scroll position).
     */
    @Override
    public void refreshItem(T item) {
        super.refreshItem(item);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A {@code null} item represents the virtual root of the hierarchy (the
     * parent of root-level items, consistent with APIs such as
     * {@link TreeData#addItem(Object, Object)}). With {@code refreshChildren}
     * {@code true}, root-level children are re-fetched from the data provider
     * without performing a full {@link #refreshAll()}. With
     * {@code refreshChildren} {@code false}, only currently cached root-level
     * items are re-rendered in place.
     *
     * @throws UnsupportedOperationException
     *             if the hierarchy format is not {@link HierarchyFormat#NESTED}
     *             and {@code item} is not {@code null} while
     *             {@code refreshChildren} is {@code true}
     * @since 25.0
     */
    @Override
    public void refreshItem(T item, boolean refreshChildren) {
        // Virtual root (null) is always allowed: re-fetching root-level data is
        // valid for every hierarchy format. Non-null items with refreshChildren
        // remain NESTED-only (same as HierarchicalDataCommunicator).
        if (item != null && refreshChildren
                && !getHierarchyFormat().equals(HierarchyFormat.NESTED)) {
            throw new UnsupportedOperationException(
                    """
                            Refreshing children of an item is only supported when the data provider \
                            uses HierarchyFormat#NESTED. For other formats, use refreshAll() instead.
                            """);
        }

        super.refreshItem(item, refreshChildren);
    }

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
