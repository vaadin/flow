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
 * immediately before a count query is issued to a data provider, on the thread
 * that issues it.
 *
 * @see AbstractDataCountEvent
 * @since 25.3
 */
public class DataCountStartedEvent extends AbstractDataCountEvent {

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
     */
    public DataCountStartedEvent(UI ui, Component component, boolean filtered) {
        super(ui, component, filtered);
    }
}
