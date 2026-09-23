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
 * once a count query has returned or thrown, on the thread that issued it.
 * <p>
 * Fired in reverse listener registration order, so that listeners nest around
 * the matching {@link DataCountStartedEvent}. It is fired whether the query
 * returned normally or threw, so a listener can clean up its timing state in
 * one place.
 *
 * @see AbstractDataCountEvent
 * @since 25.3
 */
public class DataCountEndedEvent extends AbstractDataCountEvent {

    private final int count;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the counting component belongs to, not {@code null}
     * @param component
     *            the component whose data is being counted, or {@code null} if
     *            it could not be resolved
     * @param filtered
     *            whether a filter was set on the query
     * @param count
     *            the number of items the data provider reported, or {@code -1}
     *            if the query threw, in which case a
     *            {@link DataCountFailedEvent} carrying the throwable was fired
     *            first
     */
    public DataCountEndedEvent(UI ui, Component component, boolean filtered,
            int count) {
        super(ui, component, filtered);
        this.count = count;
    }

    /**
     * Gets the number of items the data provider reported.
     *
     * @return the count, or {@code -1} if the query threw
     */
    public int getCount() {
        return count;
    }
}
