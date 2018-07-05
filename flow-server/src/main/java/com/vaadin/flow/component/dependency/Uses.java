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
package com.vaadin.flow.component.dependency;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;

/**
 * Marks that an annotated component implicitly uses another component. This
 * will ensure that any dependencies of the used component are also loaded. For
 * {@link PolymerTemplate} implementations, used components will also be
 * instantiated if an element with the corresponding {@link Tag @Tag} value is
 * defined in the template.
 * <p>
 * Marking class A with <code>@Uses(B.class)</code> will ensure all
 * {@link StyleSheet}, {@link HtmlImport}, {@link JavaScript} dependencies for
 * class <code>B</code> are loaded when class <code>A</code> is used.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
     * Internal annotation to enable use of multiple {@link Uses} annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link Uses} annotations.
         *
         * @return an array of the uses annotations
         */
        Uses[] value();
    }
}
