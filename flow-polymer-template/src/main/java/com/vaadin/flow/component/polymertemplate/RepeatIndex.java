/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
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
 * @deprecated dom-repeat is not support by Lit templates but you may still use
 *             {@code @EventData("some_data")} directly to receive data from the
 *             client side. Polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface RepeatIndex {
}
