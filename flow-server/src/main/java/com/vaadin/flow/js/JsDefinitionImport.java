/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.js;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.dependency.JsModule;

/**
 * Gives the {@link JsExpression JavaScript} of a {@link JsDefinition} that
 * extends {@link JsInvokers} names from a JavaScript module, so an expression
 * can call what the module exports without the module publishing it on
 * <code>window</code>.
 *
 * <pre>
 * &#64;JsDefinition
 * &#64;JsDefinitionImport(from = "./gridConnector.js", namespace = "connector")
 * public interface GridConnectorJs extends JsInvokers {
 *
 *     &#64;JsExpression("connector.initLazy(this)")
 *     void initLazy();
 * }
 * </pre>
 *
 * The attributes name what is imported, like the parts of a JavaScript
 * <code>import</code> statement:
 * <code>&#64;JsDefinitionImport(from = "./my.js", defaultExport = "a", namespace = "b", exports = { "c", "d" })</code>
 * gives the expressions what
 * <code>import a, { c, d }, * as b from "./my.js"</code> would. Each attribute
 * is optional and they can be combined, but at least one name is needed: a
 * module that is only loaded for its side effects is declared with
 * {@link JsModule} instead.
 * <p>
 * The default export is reached through {@link #defaultExport()}, or as
 * <code>default</code> of the {@link #namespace()}. A named export keeps its
 * name and can not be renamed. When two modules export the same name, import
 * one of them as a {@link #namespace()} instead.
 * <p>
 * The module is loaded the first time a method of the definition that declares
 * the import is called, not when the application starts, so a module that only
 * some views need does not slow down the others. Later calls use the loaded
 * module. A {@link JsModule} on the same interface is still loaded eagerly.
 * <p>
 * {@link #from()} is resolved the way {@link JsModule#value()} is: a path
 * starting with <code>./</code> names a file in the frontend folder of the
 * project or in <code>META-INF/resources/frontend</code> of a jar, and any
 * other path names an npm package or a file in one.
 * <p>
 * The imports belong to the interface they are declared on. A method inherited
 * from another interface sees the imports of the interface that declares it,
 * not those of the interface it is called through. Several annotations can
 * import from the same module; they are loaded as one module.
 * <p>
 * The names are given to the expression as parameters, so each one has to be a
 * JavaScript identifier that is not a reserved word, such as
 * <code>default</code> or <code>this</code>, and not <code>$0</code>,
 * <code>$1</code>, &hellip;, which name the arguments of the call. A name can
 * be declared only once by the imports of an interface. The expression runs in
 * a function of its own, so <code>arguments</code> in it holds the imported
 * values rather than the arguments of the call: use <code>$0</code>,
 * <code>$1</code>, &hellip; instead.
 * <p>
 * The annotation applies to an interface whose methods are called through a
 * definition that extends {@link JsInvokers}, the definition itself or an
 * interface it extends, and is ignored, with an error in the log of the build,
 * on a class. The functions of a {@link JsSnippets} interface are not
 * asynchronous and can not wait for a module to load, and an interface
 * annotated with {@link JsStringFallback} stands in for JavaScript that names
 * nothing of a module, so both are refused, as is an annotation that names
 * nothing.
 *
 * @see JsDefinition
 * @see JsInvokers
 * @see JsModule
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JsDefinitionImport.Container.class)
public @interface JsDefinitionImport {

    /**
     * The module to import from, resolved the way {@link JsModule#value()} is.
     *
     * @return the path of the module
     */
    String from();

    /**
     * The name the default export of the module is given, like <code>a</code>
     * in <code>import a from "./my.js"</code>. Empty for none.
     *
     * @return the name of the default export, or an empty string
     */
    String defaultExport() default "";

    /**
     * The name the whole module is given, like <code>b</code> in
     * <code>import * as b from "./my.js"</code>. Empty for none.
     *
     * @return the name of the module, or an empty string
     */
    String namespace() default "";

    /**
     * The named exports of the module to import, each under its own name, like
     * <code>c</code> and <code>d</code> in
     * <code>import { c, d } from "./my.js"</code>.
     *
     * @return the names of the exports, empty for none
     */
    String[] exports() default {};

    /**
     * Internal annotation to enable use of multiple {@link JsDefinitionImport}
     * annotations.
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {

        /**
         * Internally used to enable use of multiple {@link JsDefinitionImport}
         * annotations.
         *
         * @return the imports
         */
        JsDefinitionImport[] value();
    }
}
