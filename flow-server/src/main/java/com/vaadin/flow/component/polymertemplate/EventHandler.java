/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.dom.DisabledUpdateMode;

/**
 * Publishes the annotated method so it can be invoked from the client side as
 * template event handlers.
 *
 * Recommend using {@code LitTemplate} instead of PolymerTemplate as Polymer
 * will be deprecated in the next LTS version.
 *
 * @see EventData
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface EventHandler {

    /**
     * Controls event handling for the method from the client side to the server
     * side when the element is disabled.
     *
     * @return the property update mode for disabled element
     */
    DisabledUpdateMode value() default DisabledUpdateMode.ONLY_WHEN_ENABLED;
}
