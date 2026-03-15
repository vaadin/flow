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
package com.vaadin.flow.data.binder;

import java.util.Collection;
import java.util.List;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataViewUtils;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * A generic interface for listing components that use a data provider for
 * showing data.
 * <p>
 * A listing component should implement either this interface or
 * {@link HasFilterableDataProvider}, but not both.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the item data type
 *
 * @see HasFilterableDataProvider
 */
public interface HasDataProvider<T> extends HasItems<T> {

    /**
     * Sets the data provider for this listing. The data provider is queried for
     * displayed items as needed.
     *
     * @param dataProvider
     *            the data provider, not null
     * @throws BindingActiveException
     *             if there is an active signal binding for items
     */
    void setDataProvider(DataProvider<T, ?> dataProvider);

    @Override
    default void setItems(Collection<T> items) {
        DataViewUtils.checkNoActiveItemsBinding(this);
        setDataProvider(DataProvider.ofCollection(items));
    }

    /**
     * Binds a signal containing a list of item signals to this component,
     * enabling fine-grained reactive updates. This method establishes a
     * reactive binding where:
     * <ul>
     * <li>Changes to the outer signal (list structure changes such as adding or
     * removing items) trigger {@link DataProvider#refreshAll()}</li>
     * <li>Changes to any inner signal (individual item value changes) trigger
     * {@link DataProvider#refreshItem(Object)} for that specific item</li>
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
     * @throws IllegalArgumentException
     *             if this is not a Component instance
     * @throws BindingActiveException
     *             if there is already an active items binding
     */
    default void bindItems(
            Signal<? extends List<? extends Signal<T>>> itemsSignal) {
        DataViewUtils.bindItems(this, itemsSignal);
    }

}
