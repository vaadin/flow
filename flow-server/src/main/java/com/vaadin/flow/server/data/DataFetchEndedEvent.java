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

import java.time.Duration;

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
 * <p>
 * The event tells how long the fetch took and, through a
 * {@link #getRowsReturned() row count} of {@code -1}, whether it threw, so a
 * listener that times fetches needs no state of its own and no listener for the
 * started or failed event.
 *
 * @see AbstractDataFetchEvent
 * @since 25.3
 */
public class DataFetchEndedEvent extends AbstractDataFetchEvent {

    private final int rowsReturned;
    private final Duration duration;

    /**
     * Creates a new event for a fetch whose duration was not measured.
     * <p>
     * The event reports a {@link Duration#ZERO zero} duration. Use
     * {@link #DataFetchEndedEvent(UI, Component, int, int, boolean, int, Duration)}
     * to create an event with a measured duration.
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
        this(ui, component, offset, limit, filtered, rowsReturned,
                Duration.ZERO);
    }

    /**
     * Creates a new event carrying the measured duration of the fetch.
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
     * @param duration
     *            the time from just before the started event was fired until
     *            the returned items were consumed or the fetch threw, not
     *            {@code null}
     */
    public DataFetchEndedEvent(UI ui, Component component, int offset,
            int limit, boolean filtered, int rowsReturned, Duration duration) {
        super(ui, component, offset, limit, filtered);
        this.rowsReturned = rowsReturned;
        this.duration = duration;
    }

    /**
     * Gets the number of items the data provider actually returned.
     * <p>
     * A value of {@code -1} is how this event reports that the fetch threw, so
     * a listener can tell a failed fetch from an empty page without listening
     * for {@link DataFetchFailedEvent}, which carries the throwable itself.
     *
     * @return the row count, or {@code -1} if the fetch threw
     */
    public int getRowsReturned() {
        return rowsReturned;
    }

    /**
     * Gets how long the fetch took, measured from just before the
     * {@link DataFetchStartedEvent} was fired until the returned items were
     * consumed or the fetch threw. It includes the time the listeners of the
     * started and failed events took, but not the time spent on this event.
     *
     * @return the duration, never negative
     */
    public Duration getDuration() {
        return duration;
    }
}
