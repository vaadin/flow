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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for instructing flow to import a CSS file into the application
 * bundle.
 * <p>
 * Depending on the attributes provided, the CSS content will be appended in
 * different ways:
 * 
 * <ul>
 * <li>When specifying only the 'value', it will be appended to the
 * 'document.head' inside a '&lt;custom-style>' block.
 * 
 * <pre>
 *  &lt;custom-style>
 *    &lt;style>
 *      CSS-CONTENT
 *    &lt;/style>
 *  &lt;/custom-style>
 * </pre>
 * 
 * <li>When specifying 'value' and 'include', it is appended inside a
 * '&lt;custom-style>' block, but 'include' value is used for including a
 * defined module.
 * 
 * <pre>
 *  &lt;custom-style&gt;
 *    &lt;style include="INCLUDE-VALUE"&gt;
 *      CSS-CONTENT
 *    &lt;/style&gt;
 *  &lt;/custom-style&gt;
 * </pre>
 * 
 * <li>When 'value' and 'id' are given, a new 'dom-module' with the provided
 * 'id' is created
 * 
 * <pre>
 * &lt;dom-module id="ID-VALUE"&gt;
 *   &lt;template&gt;
 *     &lt;style&gt;
 *       CSS-CONTENT
 *     &lt;/style&gt;
 *   &lt;/template&gt;
 * &lt;/dom-module&gt;
 * </pre>
 * 
 * <li>When 'value' and 'themeFor' are given, a new 'dom-module' for customizing
 * a themable element is created
 * 
 * <pre>
 * &lt;dom-module id="RANDOM-ID" theme-for="THEME-FOR-VALUE"&gt;
 *   &lt;template&gt;
 *     &lt;style&gt;
 *       CSS-CONTENT
 *     &lt;/style&gt;
 *   &lt;/template&gt;
 * &lt;/dom-module&gt;
 * </pre>
 * </ul>
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(CssImport.Container.class)
public @interface CssImport {

    /**
     * Location of the file with the CSS content.
     * 
     * @return the value.
     */
    String value();

    /**
     * The 'id' of a module to include in the generated 'custom-style'.
     * 
     * @return the include value.
     */
    String include() default "";

    /**
     * The 'id' of the new 'dom-module' created.
     * 
     * @return the id.
     */
    String id() default "";

    /**
     * The tag name of the themable element that the generated 'dom-module' will
     * target.
     * 
     * @return the themable element.
     */
    String themeFor() default "";

    /**
     * Internal annotation to enable use of multiple {@link CssImport}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @interface Container {
        /**
         * Internally used to enable use of multiple {@link CssImport}
         * annotations.
         *
         * @return an array of the JavaScript annotations
         */
        CssImport[] value();
    }
}
