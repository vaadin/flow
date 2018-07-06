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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines files to inline into the initial page.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(Inline.Container.class)
public @interface Inline {
    /**
     * File content wrapping enum.
     */
    enum Wrapping {
        AUTOMATIC, NONE, JAVASCRIPT, STYLESHEET
    }

    /**
     * Inline position enum.
     */
    enum Position {
        PREPEND, APPEND
    }

    /**
     * Classpath file to inline into target element.
     * 
     * @return file to inline
     */
    String value();

    /**
     * Target element to inline file contents to.
     * 
     * @return inline target
     */
    TargetElement target() default TargetElement.HEAD;

    /**
     * Inline position of element. Default appends to target element.
     * 
     * @return inline position
     */
    Position position() default Position.APPEND;

    /**
     * The element type to inline as. Default is automatic which tries to figure
     * out the correct type by file ending.
     * 
     * @return inline element type
     */
    Wrapping wrapping() default Wrapping.AUTOMATIC;

    /**
     * Internal annotation to enable use of multiple {@link Inline} annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @interface Container {

        /**
         * Internally used to enable use of multiple {@link Inline} annotations.
         *
         * @return an array of the style sheet annotations
         */
        Inline[] value();
    }
}
