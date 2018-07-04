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
 * Defines the access control setting for a model property. By default, updates
 * are allowed only for properties that are defined with a two-way binding in
 * the template. Two-way bindings are defined using the
 * <code>{{propertyName}}</code> or <code>{{propertyName:updatingEvent</code>
 * syntax in templates.
 * <p>
 * Use this annotation on accessors in your {@link TemplateModel} class to
 * define whether it's allowed for the client to update server-side model values
 *
 * @see ClientUpdateMode
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(AllowClientUpdates.Container.class)
@Documented
public @interface AllowClientUpdates {

    /**
     * The client update mode to use for the designated property.
     *
     * @return the client update mode to use.
     */
    ClientUpdateMode value() default ClientUpdateMode.ALLOW;

    /**
     * A dot separated path of the sub property that this access control setting
     * applies to. Empty string by default, which will apply the access control
     * directly to the annotated property.
     *
     * @return the dot separated path to the bean property for which the access
     *         control applies, empty string by default
     */
    String path() default "";

    /**
     * Internal annotation to enable use of multiple {@link AllowClientUpdates}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link AllowClientUpdates}
         * annotations.
         *
         * @return an array of the AccessControl annotations
         */
        AllowClientUpdates[] value();
    }
}
