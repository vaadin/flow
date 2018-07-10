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
package com.vaadin.flow.component.page;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a meta tag with customized name and content that will be added to the
 * HTML of the host page of a UI class.
 *
 * @author Vaadin Ltd
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Repeatable(Meta.Container.class)
public @interface Meta {
    /**
     * Gets the custom tag name.
     * 
     * @return the custom tag name
     */
    String name();

    /**
     * Gets the custom tag content.
     * 
     * @return the custom tag content
     */
    String content();

    /**
     * Internal annotation to enable use of multiple {@link Meta} annotations.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    public @interface Container {
        /**
         * Internally used to enable use of multiple {@link Meta} annotations.
         *
         * @return an array of the Meta annotations
         */
        Meta[] value();
    }
}
