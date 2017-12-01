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
package com.vaadin.ui.common;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.Component;

/**
 * Marks that a component should load all the dependencies for another
 * component.
 * <p>
 * Marking class A with <code>@Uses(B.class)</code> will ensure all
 * {@link StyleSheet}, {@link HtmlImport}, {@link JavaScript} dependencies for
 * class <code>B</code> are loaded when class <code>A</code> is used.
 *
 * @author Vaadin Ltd
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Uses.Container.class)
public @interface Uses {

    /**
     * Marks the component class to depend on.
     *
     * @return the component class to depend on
     */
    Class<? extends Component> value();

    /**
     * Annotation enabling using multiple {@link Uses @Uses} annotations.
     * <p>
     * <b>NOT meant to be used directly</b>
     *
     * @author Vaadin Ltd
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Container {

        /**
         * Not to be used directly, use {@link Uses @Uses} instead.
         *
         * @return an array of the uses annotations
         */
        Uses[] value();
    }
}
