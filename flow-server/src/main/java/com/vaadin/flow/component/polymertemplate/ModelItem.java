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

/**
 * Allows to receive Model class item from the client side instead of a index of
 * an element in dom-repeat Polymer template section, or string representation.
 * <p>
 * Can be applied on parameters with classes found in the {@code TemplateModel}.
 * <p>
 * This is by default a shorthand for {@code @EventData("event.model.item")}
 * that works with {@code List} type model items. For other Model items define
 * the value to be the methodName e.g. for {@code setSubItem(...)} use
 * {@code @ModelItem("subItem")}.
 *
 * Recommend using {@code LitTemplate} instead of PolymerTemplate as Polymer
 * will be deprecated in the next LTS version.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface ModelItem {

    /**
     * Path value for ModelItem.
     *
     * @return Given Path or default value
     */
    String value() default "event.model.item";
}
