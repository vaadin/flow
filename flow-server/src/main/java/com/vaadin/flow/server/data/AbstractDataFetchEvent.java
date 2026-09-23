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

import java.util.EventObject;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Details shared by the events fired around a fetch query issued to a data
 * provider, that is a query loading one page of items.
 * <p>
 * {@link #getOffset()} and {@link #getLimit()} say what was asked for;
 * {@link DataFetchEndedEvent#getRowsReturned()} says what came back. A
 * component that repeatedly asks for far more than it renders, or a data
 * provider that returns short pages, both show up as a gap between the two.
 * <p>
 * The events carry only plain values and the resolved {@link Component}, never
 * the query or the data provider, so that observers cannot retain or mutate
 * loading state.
 * <p>
 * <b>This type cannot be listened for.</b> The
 * {@link VaadinService#getEventBus() service event bus} dispatches by exact
 * runtime type, so a listener registered for this class is never notified;
 * register for {@link DataFetchStartedEvent}, {@link DataFetchFailedEvent} or
 * {@link DataFetchEndedEvent} instead.
 *
 * @since 25.3
 */
public abstract class AbstractDataFetchEvent extends EventObject {

    private final transient Component component;
    private final int offset;
    private final int limit;
    private final boolean filtered;

    /**
     * Creates a new fetch event.
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
     */
    protected AbstractDataFetchEvent(UI ui, Component component, int offset,
            int limit, boolean filtered) {
        super(ui);
        this.component = component;
        this.offset = offset;
        this.limit = limit;
        this.filtered = filtered;
    }

    /**
     * Gets the UI from which this event originates.
     *
     * @return the UI, never {@code null}
     */
    @Override
    public UI getSource() {
        return (UI) super.getSource();
    }

    /**
     * Gets the UI from which this event originates.
     *
     * @return the UI, never {@code null}
     */
    public UI getUI() {
        return getSource();
    }

    /**
     * Gets the component whose data is being fetched, so that an observer can
     * attribute the query to a {@code Grid}, {@code ComboBox} or
     * {@code TreeGrid} rather than only to the request.
     * <p>
     * Empty when the component could not be resolved from the state node, for
     * example when the data communicator is driven by a bare element.
     *
     * @return the component, or an empty optional
     */
    public Optional<Component> getComponent() {
        return Optional.ofNullable(component);
    }

    /**
     * Gets the index of the first item requested.
     *
     * @return the offset, never negative
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the number of items requested. The data provider may return fewer;
     * compare against {@link DataFetchEndedEvent#getRowsReturned()}.
     *
     * @return the limit, never negative
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Tells whether a filter was set on the query, which distinguishes a combo
     * box loading matches for what the user typed from one loading the whole
     * data set.
     *
     * @return {@code true} if the query carried a filter
     */
    public boolean isFiltered() {
        return filtered;
    }
}
