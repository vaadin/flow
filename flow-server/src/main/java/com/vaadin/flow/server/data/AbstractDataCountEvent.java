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
 * Details shared by the events fired around a count query issued to a data
 * provider, that is a query asking how many items a level holds rather than for
 * the items themselves.
 * <p>
 * A flat component issues one count query per level reload. A hierarchical
 * component issues one per expanded parent, so several count events for one
 * component within a single request is the signature of an expensive hierarchy.
 * <p>
 * The events carry only plain values and the resolved {@link Component}, never
 * the query or the data provider, so that observers cannot retain or mutate
 * loading state.
 * <p>
 * <b>This type cannot be listened for.</b> The
 * {@link VaadinService#getEventBus() service event bus} dispatches by exact
 * runtime type, so a listener registered for this class is never notified;
 * register for {@link DataCountStartedEvent}, {@link DataCountFailedEvent} or
 * {@link DataCountEndedEvent} instead.
 *
 * @since 25.3
 */
public abstract class AbstractDataCountEvent extends EventObject {

    private final transient Component component;
    private final boolean filtered;

    /**
     * Creates a new count event.
     *
     * @param ui
     *            the UI the counting component belongs to, not {@code null}
     * @param component
     *            the component whose data is being counted, or {@code null} if
     *            it could not be resolved
     * @param filtered
     *            whether a filter was set on the query
     */
    protected AbstractDataCountEvent(UI ui, Component component,
            boolean filtered) {
        super(ui);
        this.component = component;
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
     * Gets the component whose data is being counted, so that an observer can
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
     * Tells whether a filter was set on the query, which distinguishes a combo
     * box counting matches for what the user typed from one counting the whole
     * data set.
     *
     * @return {@code true} if the query carried a filter
     */
    public boolean isFiltered() {
        return filtered;
    }
}
