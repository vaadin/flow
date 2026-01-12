/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DomListenerRegistration;

/**
 * Maps data from a DOM event to a {@link ComponentEvent}.
 * <p>
 * This annotation should be added to parameters in the DOM event constructor in
 * a {@link ComponentEvent}, mapped using @{@link DomEvent}. See
 * the @{@link DomEvent} documentation for more information.
 * <p>
 * The annotation {@link #value()} will be evaluated as JavaScript when the
 * event is handled in the browser. The expression is evaluated in a context
 * where <code>element</code> refers to the element for which the listener is
 * registered and <code>event</code> refers to the fired event. The value of the
 * expression is passed back to the server and injected into the annotated
 * {@link ComponentEvent} constructor parameter.
 * <p>
 * Supported parameter types include:
 * <ul>
 * <li>Primitives and their wrappers: {@link Integer}, {@link Double},
 * {@link Boolean}, int, double, boolean, etc.</li>
 * <li>String values: {@link String}</li>
 * <li>JSON types: {@link tools.jackson.databind.JsonNode}</li>
 * <li>Bean/DTO types: Any Java bean or record that can be deserialized from
 * JSON using Jackson</li>
 * <li>Collections: {@link java.util.List}, {@link java.util.Map}, etc. (when
 * using generic types with proper bean definitions)</li>
 * </ul>
 * <p>
 * Example with a bean type:
 *
 * <pre>
 * public class MouseDetails {
 *     private int clientX;
 *     private int clientY;
 *     // getters and setters
 * }
 *
 * &#64;DomEvent("custom-click")
 * public class CustomClickEvent extends ComponentEvent&lt;Component&gt; {
 *     public CustomClickEvent(Component source, boolean fromClient,
 *             &#64;EventData("event.detail") MouseDetails details) {
 *         super(source, fromClient);
 *         // details is automatically deserialized from the event data
 *     }
 * }
 * </pre>
 *
 * @see DomEvent
 * @see DomListenerRegistration#addEventData(String)
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface EventData {
    /**
     * A JavaScript expression that will be evaluated to collect data when an
     * event is handled.
     *
     * @see DomListenerRegistration#addEventData(String)
     *
     * @return the expression to use for fetching event data
     */
    String value();
}
