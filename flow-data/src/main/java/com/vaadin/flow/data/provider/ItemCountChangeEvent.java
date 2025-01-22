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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Event describing the item count change for a component. The
 * {@link ItemCountChangeEvent} will fired during the "before client
 * response"-phase, so changes done during the server round trip will only
 * receive one event. For example, this code will trigger only one event,
 * although there are two methods called which cause the item count change:
 *
 * <pre>
 * {@code
 * dataView.addItemCountChangeListener(listener);
 * dataView.addItem(newItem);
 * dataView.setFilter(filter);
 * }
 * </pre>
 *
 * @param <T>
 *            the event source type
 * @since
 */
public class ItemCountChangeEvent<T extends Component>
        extends ComponentEvent<T> {
    private final int itemCount;
    private final boolean itemCountEstimated;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source
     *            the source component
     * @param itemCount
     *            new items count
     * @param itemCountEstimated
     *            whether item count is an estimate
     */
    public ItemCountChangeEvent(T source, int itemCount,
            boolean itemCountEstimated) {
        super(source, false);
        this.itemCount = itemCount;
        this.itemCountEstimated = itemCountEstimated;
    }

    /**
     * Get the new item count for the component.
     *
     * @return items count
     */
    public int getItemCount() {
        return itemCount;
    }

    /**
     * Returns whether the item count {@link #getItemCount()} is an estimate or
     * the exact count. An estimate is used when items are fetched lazily from
     * the backend and the count callback has not been provided. See further
     * details from {@link LazyDataView#setItemCountEstimate(int)}.
     * <p>
     * <em>NOTE:</em> this only applies for components that do lazy loading from
     * the backend and implement {@link HasLazyDataView}.
     *
     * @return {@code true} when the count is an estimate, {@code false} when
     *         the count is exact
     */
    public boolean isItemCountEstimated() {
        return itemCountEstimated;
    }
}
