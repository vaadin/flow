/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a ModelConverter on a template model property found through
 * {@link #path()}.
 * <p>
 * Use this annotation on setters in your {@link TemplateModel} class to perform
 * type conversions on properties.
 *
 * @see ModelConverter
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(InternalContainerAnnotationForConvert.class)
public @interface Convert {

    /**
     * The ModelConverter class to use for conversion of the property found
     * through {{@link #path()}.
     *
     * @return the ModelConverter class
     */
    Class<? extends ModelConverter<?, ?>> value();

    /**
     * The dot separated path from the TemplateModel property to the value to
     * apply conversion to. Empty string by default, which will apply conversion
     * directly to the property.
     *
     * @return the dot separated path to the bean property to convert, empty
     *         string by default
     */
    String path() default "";
}
