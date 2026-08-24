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
package com.vaadin.flow.server.data;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * once a page of items has been loaded and consumed, or the fetch threw, on the
 * thread that requested it.
 * <p>
 * Fired in reverse listener registration order, so that listeners nest around
 * the matching {@link DataFetchStartedEvent}. Because a data provider may
 * return a lazily evaluated {@link java.util.stream.Stream}, this is fired
 * after the returned items have been consumed, so the measured duration covers
 * the backend round-trip rather than only the call that started it.
 *
 * @see AbstractDataFetchEvent
 * @since 25.3
 */
public class DataFetchEndedEvent extends AbstractDataFetchEvent {

    private final int rowsReturned;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the fetching component belongs to, not {@code null}
     * @param component
     *            the component whose data is being fetched, or {@code null} if
     *            it could not be resolved
     * @param offset
     *            the index of the first item requested
     * @param limit
     *            the number of items requested
     * @param filtered
     *            whether a filter was set on the query
     * @param rowsReturned
     *            the number of items the data provider actually returned, which
     *            may be fewer than {@link #getLimit()}, or {@code -1} if the
     *            fetch threw, in which case a {@link DataFetchFailedEvent}
     *            carrying the throwable was fired first
     */
    public DataFetchEndedEvent(UI ui, Component component, int offset,
            int limit, boolean filtered, int rowsReturned) {
        super(ui, component, offset, limit, filtered);
        this.rowsReturned = rowsReturned;
    }

    /**
     * Gets the number of items the data provider actually returned.
     *
     * @return the row count, or {@code -1} if the fetch threw
     */
    public int getRowsReturned() {
        return rowsReturned;
    }
}
