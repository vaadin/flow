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
package com.vaadin.flow.templatemodel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines which properties to include when importing a bean into a template
 * model.
 * <p>
 * Use this annotation on bean setters in your {@link TemplateModel} class to
 * restrict which properties of the beans are imported into the model.
 * <p>
 * You can only define exact matches using this filter. If you need more
 * control, you can use
 * {@link TemplateModel#importBean(String, Object, java.util.function.Predicate)}
 * and define a custom filter.
 * <p>
 * Note that <code>@Include</code> annotations are not inherited.
 *
 * @see Exclude
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Include {

    /**
     * Properties to include from a bean when importing into a template model.
     * <p>
     * By default all properties are included.
     *
     * @return the properties to include from a bean when importing into a
     *         template model
     */
    String[] value();
}
