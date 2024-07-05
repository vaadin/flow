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

/**
 * Allows to receive index of an element in dom-repeat Polymer template section.
 *
 * Can be applied on parameters of {@code int} and {@link Integer} types.
 *
 * This is a shorthand for {@code @EventData("event.model.index")}, for more
 * information, refer to {@link EventData}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface RepeatIndex {
}
