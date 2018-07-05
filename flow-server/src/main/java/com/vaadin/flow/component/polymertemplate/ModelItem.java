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
