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
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Imports a CSS file into the application bundle.
 * <p>
 * The CSS files should be located in the place as JS module files:
 * <ul>
 * <li>inside {@code frontend} directory in your root project folder in case of
 * WAR project
 * <li>inside {@code META-INF/resources/frontend} directory (inside a project
 * resources folder) in case of JAR project (if you are using Maven this is
 * {@code src/main/resources/META-INF/resources/frontend} directory).
 * </ul>
 * <p>
 * The annotation doesn't have any effect in the compatibility mode: use it only
 * for Polymer 3 templates.
 * <p>
 * Depending on the attributes provided, the CSS content will be appended in
 * different ways:
 *
 *
 * <ul>
 *
 * <li>When specifying only the 'value', it will be appended to the
 * 'document.head' inside a '&lt;custom-style&gt;' block.
 *
 *
 *
 * <pre>
 *  &lt;custom-style&gt;
 *    &lt;style&gt;
 *      CSS-CONTENT
 *    &lt;/style&gt;
 *  &lt;/custom-style&gt;
 *
 * </pre>
 *
 *
 *
 * <li>When specifying 'value' and 'include', it is appended inside a
 * '&lt;custom-style&gt;' block, but 'include' value is used for including a
 * defined module. Multiple modules are allowed by passing a space separated
 * list.
 *
 *
 *
 * <pre>
 *  &lt;custom-style&gt;
 *    &lt;style include="INCLUDE-VALUE"&gt;
 *      CSS-CONTENT
 *    &lt;/style&gt;
 *  &lt;/custom-style&gt;
 *
 * </pre>
 *
 *
 *
 * <li>When 'value' and 'id' are given, a new 'dom-module' with the provided
 * 'id' is created, the 'include' parameter is allowed and is added to the
 * &lt;style&gt; tag inside the module template.
 *
 *
 *
 * <pre>
 * &lt;dom-module id="ID-VALUE"&gt;
 *   &lt;template&gt;
 *     &lt;style include="INCLUDE-VALUE"&gt;
 *       CSS-CONTENT
 *     &lt;/style&gt;
 *   &lt;/template&gt;
 * &lt;/dom-module&gt;
 *
 * </pre>
 *
 *
 *
 * <li>When 'value' and 'themeFor' are given, a new 'dom-module' for customizing
 * a themable element is created, the 'include' parameter is allowed and is
 * added to the &lt;style&gt; tag inside the module template.
 *
 *
 *
 * <pre>
 * &lt;dom-module id="RANDOM-ID" theme-for="THEME-FOR-VALUE"&gt;
 *   &lt;template&gt;
 *     &lt;style include="INCLUDE-VALUE"&gt;
 *       CSS-CONTENT
 *     &lt;/style&gt;
 *   &lt;/template&gt;
 * &lt;/dom-module&gt;
 *
 * </pre>
 *
 *
 * </ul>
 * <p>
 * It is guaranteed that dependencies will be loaded only once. The files loaded
 * will be in the same order as the annotations were on the class. However,
 * loading order is only guaranteed on a class level; Annotations from different
 * classes may appear in different order, grouped by the annotated class. Also,
 * files identified by {@code @CssImport} will be loaded after
 * {@link com.vaadin.flow.component.dependency.JsModule} and
 * {@link com.vaadin.flow.component.dependency.JavaScript}.
 * <p>
 * NOTE: Currently all frontend resources are bundled together into one big
 * bundle. This means, that CSS files loaded by one class will be present on a
 * view constructed by another class. For example, if there are two classes
 * {@code RootRoute} annotated with {@code @Route("")}, and another class
 * {@code RouteA} annotated with {@code @Route("route-a")} and
 * {@code @CssImport("./styles/custom-style.css")}, the {@code custom-style.css}
 * will be present on the root route as well.
 *
 * @since 2.0
 *
 * @see JsModule
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
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
    @Inherited
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
