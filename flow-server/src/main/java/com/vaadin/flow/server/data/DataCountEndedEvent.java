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
 * once a count query has returned or thrown, on the thread that issued it.
 * <p>
 * Fired in reverse listener registration order, so that listeners nest around
 * the matching {@link DataCountStartedEvent}. It is fired whether the query
 * returned normally or threw, so a listener can clean up its state in one
 * place.
 * <p>
 * The event tells how long the query took and, through a {@link #getCount()
 * count} of {@code -1}, whether it threw, so a listener that times count
 * queries needs no state of its own and no listener for the started or failed
 * event.
 *
 * @see AbstractDataCountEvent
 * @since 25.3
 */
public class DataCountEndedEvent extends AbstractDataCountEvent {

    private final int count;
    private final Duration duration;

    /**
     * Creates a new event for a count query whose duration was not measured.
     * <p>
     * The event reports a {@link Duration#ZERO zero} duration. Use
     * {@link #DataCountEndedEvent(UI, Component, boolean, int, Duration)} to
     * create an event with a measured duration.
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
        this(ui, component, filtered, count, Duration.ZERO);
    }

    /**
     * Creates a new event carrying the measured duration of the count query.
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
     * @param duration
     *            the time from just before the started event was fired until
     *            the query returned or threw, not {@code null}
     */
    public DataCountEndedEvent(UI ui, Component component, boolean filtered,
            int count, Duration duration) {
        super(ui, component, filtered);
        this.count = count;
        this.duration = duration;
    }

    /**
     * Gets the number of items the data provider reported.
     * <p>
     * A value of {@code -1} is how this event reports that the query threw, so
     * a listener can tell a failed query from an empty result without listening
     * for {@link DataCountFailedEvent}, which carries the throwable itself.
     *
     * @return the count, or {@code -1} if the query threw
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets how long the count query took, measured from just before the
     * {@link DataCountStartedEvent} was fired until the query returned or
     * threw. It includes the time the listeners of the started and failed
     * events took, but not the time spent on this event.
     *
     * @return the duration, never negative
     */
    public Duration getDuration() {
        return duration;
    }
}
