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

import java.io.Serializable;
import java.util.function.IntSupplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.data.DataCountEndedEvent;
import com.vaadin.flow.server.data.DataCountFailedEvent;
import com.vaadin.flow.server.data.DataCountStartedEvent;
import com.vaadin.flow.server.data.DataFetchEndedEvent;
import com.vaadin.flow.server.data.DataFetchFailedEvent;
import com.vaadin.flow.server.data.DataFetchStartedEvent;

/**
 * Runs a data provider query and reports it on the
 * {@link VaadinService#getEventBus() service event bus}.
 * <p>
 * For internal use by data communicators. Each method wraps the query it is
 * given, so the started/failed/ended sequence is defined in one place rather
 * than repeated at every call site. The {@code ended} event is fired in reverse
 * registration order, so listeners nest around the {@code started} one.
 * <p>
 * The caller supplies the UI rather than this class looking one up, because
 * {@link VaadinService#getCurrent()} is unavailable on the thread that matters:
 * {@link DataCommunicator#enablePushUpdates(java.util.concurrent.Executor)}
 * runs fetches on the supplied executor without propagating
 * {@link com.vaadin.flow.internal.CurrentInstance}. When the component is not
 * attached to a live UI the query simply runs unreported.
 *
 * @since 25.3
 */
public final class DataFetchObserver implements Serializable {

    /**
     * Resolves the event bus to report on, or {@code null} when there is
     * nothing to report to.
     */
    private static VaadinServiceEventBus getEventBus(UI ui) {
        if (ui == null) {
            return null;
        }
        VaadinSession session = ui.getSession();
        if (session == null) {
            return null;
        }
        VaadinService service = session.getService();
        if (service == null) {
            return null;
        }
        return service.getEventBus();
    }

    private DataFetchObserver() {
    }

    /**
     * Runs a count query, reporting it on the service event bus.
     *
     * @param ui
     *            the UI the counting component belongs to, or {@code null} if
     *            it is not attached
     * @param component
     *            the component whose data is being counted, or {@code null} if
     *            it could not be resolved
     * @param filtered
     *            whether the query carries a filter
     * @param query
     *            the count query to run
     * @return whatever the query returned
     */
    public static int count(UI ui, Component component, boolean filtered,
            IntSupplier query) {
        VaadinServiceEventBus eventBus = getEventBus(ui);
        if (eventBus == null) {
            return query.getAsInt();
        }
        eventBus.fireEvent(new DataCountStartedEvent(ui, component, filtered));
        // Stays -1 unless the query produces a count, which is what the event
        // contract defines as "the query failed".
        int reported = -1;
        try {
            reported = query.getAsInt();
            return reported;
        } catch (Throwable t) {
            eventBus.fireEvent(
                    new DataCountFailedEvent(ui, component, filtered, t));
            throw t;
        } finally {
            eventBus.fireEventInReverseOrder(
                    new DataCountEndedEvent(ui, component, filtered, reported));
        }
    }

    /**
     * Runs a fetch query, reporting it on the service event bus.
     * <p>
     * The query must consume the items it loads and return how many there were,
     * so that the reported duration covers the backend round-trip even for a
     * data provider that returns a lazily evaluated
     * {@link java.util.stream.Stream}.
     *
     * @param ui
     *            the UI the fetching component belongs to, or {@code null} if
     *            it is not attached
     * @param component
     *            the component whose data is being fetched, or {@code null} if
     *            it could not be resolved
     * @param range
     *            the range of items requested
     * @param filtered
     *            whether the query carries a filter
     * @param query
     *            the fetch query to run, returning the number of items loaded
     * @return whatever the query returned
     */
    public static int fetch(UI ui, Component component, Range range,
            boolean filtered, IntSupplier query) {
        VaadinServiceEventBus eventBus = getEventBus(ui);
        if (eventBus == null) {
            return query.getAsInt();
        }
        int offset = range.getStart();
        int limit = range.length();
        eventBus.fireEvent(new DataFetchStartedEvent(ui, component, offset,
                limit, filtered));
        int reported = -1;
        try {
            reported = query.getAsInt();
            return reported;
        } catch (Throwable t) {
            eventBus.fireEvent(new DataFetchFailedEvent(ui, component, offset,
                    limit, filtered, t));
            throw t;
        } finally {
            eventBus.fireEventInReverseOrder(new DataFetchEndedEvent(ui,
                    component, offset, limit, filtered, reported));
        }
    }
}
