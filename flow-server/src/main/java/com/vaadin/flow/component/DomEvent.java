/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;

/**
 * Maps a DOM event to a {@link ComponentEvent}.
 * <p>
 * Marking a {@link ComponentEvent} class with @{@link DomEvent} will cause the
 * {@link ComponentEvent} to be fired whenever the DOM event occurs.
 * <p>
 * A {@link ComponentEvent} class mapped with @{@link DomEvent} must have a
 * special constructor which is invoked by the framework when creating and
 * firing a {@link ComponentEvent} based on a DOM event.
 * <ul>
 * <li>The first parameter must be the event source, a {@link Component}.
 * <li>The second parameter must be a boolean, indicating whether the event
 * originated from the client (always true when fired based on a DOM event)
 * <li>Any additional parameters must be annotated using @{@link EventData},
 * telling the framework which part of the DOM event data object to map to the
 * parameter.
 * </ul>
 *
 * @see EventData
 * @see Element#addEventListener(String, com.vaadin.flow.dom.DomEventListener,
 *      String...)
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface DomEvent {
    /**
     * The name of the DOM event which should fire the annotated component
     * event.
     *
     * @return the name of the DOM event
     */
    String value();

    /**
     * Controls RPC for the listener when the element is disabled.
     *
     * @see DomListenerRegistration#setDisabledUpdateMode(DisabledUpdateMode)
     *
     * @return the property update mode for disabled element
     */
    DisabledUpdateMode allowUpdates() default DisabledUpdateMode.ONLY_WHEN_ENABLED;

    /**
     * The filter expression to run in the browser to determine whether fired
     * events should be passed to the server.
     *
     * @see DomListenerRegistration#setFilter(String)
     *
     * @return the filter expression to use, or empty string to not use any
     *         filtering
     */
    String filter() default "";

    /**
     * The debounce settings to use with this event. By default, debounce is not
     * used.
     *
     * @see DomListenerRegistration#debounce(int,
     *      com.vaadin.flow.dom.DebouncePhase,
     *      com.vaadin.flow.dom.DebouncePhase...)
     *
     * @return the debounce settings
     */
    DebounceSettings debounce() default @DebounceSettings(timeout = 0, phases = DebouncePhase.LEADING);
}
