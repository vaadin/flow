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
package com.vaadin.flow.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEventListener;
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
 * @see Element#addEventListener(String, DomEventListener)
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
     * events should be passed to the server. The event is sent only if the
     * expression returns a truthy value. The expression is evaluated in a
     * context where <code>element</code> refers to the element for which the
     * listener is registered and <code>event</code> refers to the fired event.
     * <p>
     * A filter that is a property path, which is <code>event</code> or
     * <code>element</code> followed by property names separated with
     * <code>.</code> or <code>?.</code>, is read in the browser without
     * compiling it. Many filters can be written as a path, as the value only
     * has to be truthy:
     * <ul>
     * <li><code>event.key?.length</code> instead of
     * <code>typeof(event.key) === 'string' &amp;&amp; event.key.length &gt;
     * 0</code>, for a keyboard event with a non-empty key</li>
     * <li><code>event.detail</code> instead of <code>event.detail !==
     * 0</code>, for a mouse event with a non-zero click count</li>
     * </ul>
     * Any other filter is put into the frontend bundle as a function when the
     * application is built, and the browser runs that function. Both work with
     * a Content Security Policy without <code>unsafe-eval</code> and in the
     * {@link com.vaadin.flow.server.CspMode#STRICT} mode. If the bundle does
     * not have the function, for example because the filter changed after the
     * bundle was built, the browser compiles the filter from a string, which
     * such a policy does not allow.
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

    /**
     * Controls if the event is let to propagate to parent elements.
     *
     * @return true if the event is not let to propagate to parent elements
     * @since 24.2
     */
    public boolean stopPropagation() default false;

    /**
     * Controls if the browser is asked to prevent the default browser behavior.
     *
     * @return true if default behavior is should be prevented
     * @since 24.2
     */
    public boolean preventDefault() default false;

    /**
     * Controls whether this event listener should bypass the server-side
     * security checks for modality. When set to {@code true}, the listener will
     * receive events even when the component is behind a modal component's
     * modality curtain (i.e., when the component is inert).
     * <p>
     * Handle with care! This should only be used for cases where the event
     * needs to be processed regardless of the component's modal state, such as
     * geolocation updates or other non-UI related events.
     *
     * @see DomListenerRegistration#allowInert()
     *
     * @return {@code true} if the listener should receive events even when the
     *         component is inert, {@code false} otherwise (default)
     * @since 25.1
     */
    public boolean allowInert() default false;

}
