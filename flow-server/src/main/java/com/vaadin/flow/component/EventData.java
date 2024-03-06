/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DomListenerRegistration;

import elemental.json.JsonValue;

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
 * Supported parameter types are {@link String}, {@link JsonValue},
 * {@link Integer}, {@link Double}, {@link Boolean} and their respective
 * primitive types.
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
