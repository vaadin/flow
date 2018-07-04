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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a ModelEncoder on a template model property found through
 * {@link #path()}.
 * <p>
 * Use this annotation on setters in your {@link TemplateModel} class to perform
 * type conversions on properties.
 *
 * @see ModelEncoder
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Encode.Container.class)
public @interface Encode {

    /**
     * The ModelEncoder class to use for encoding the property found through
     * {{@link #path()}.
     *
     * @return the ModelEncoder class
     */
    Class<? extends ModelEncoder<?, ?>> value();

    /**
     * The dot separated path from the TemplateModel property to the value to
     * apply encoding to. Empty string by default, which will apply encoding
     * directly to the property.
     *
     * @return the dot separated path to the bean property to encode, empty
     *         string by default
     */
    String path() default "";

    /**
     * Internal annotation to enable use of multiple {@link Encode} annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link Encode} annotations.
         *
         * @return an array of the Encode annotations
         */
        Encode[] value();
    }
}
