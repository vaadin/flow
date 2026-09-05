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
package com.vaadin.flow.component.dependency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;

/**
 * Annotation for defining JavaScript Module dependencies on a {@link Component}
 * class. For adding multiple JavaScript Module files for a single component,
 * you can use this annotation multiple times.
 * <p>
 * The JavaScript module files should be located:
 * <ul>
 * <li>inside {@code frontend} directory in your root project folder in case of
 * WAR project
 * <li>inside {@code META-INF/resources/frontend} directory (inside a project
 * resources folder) in case of JAR project (if you are using Maven this is
 * {@code src/main/resources/META-INF/resources/frontend} directory).
 * </ul>
 * <p>
 * It is guaranteed that dependencies will be loaded only once. The files loaded
 * will be in the same order as the annotations were on the class. However,
 * loading order is only guaranteed on a class level; Annotations from different
 * classes may appear in different order, grouped by the annotated class. Also,
 * files identified by {@code @JsModule} will be loaded before
 * {@link com.vaadin.flow.component.dependency.JavaScript} and
 * {@link com.vaadin.flow.component.dependency.CssImport}.
 * <p>
 * NOTE: Currently all frontend resources are bundled together into one big
 * bundle. This means, that JavaScript files loaded by one class will be present
 * on a view constructed by another class. For example, if there are two classes
 * {@code RootRoute} annotated with {@code @Route("")}, and another class
 * {@code RouteA} annotated with {@code @Route("route-a")} and
 * {@code @JsModule("./src/jsmodule.js")}, the {@code jsmodule.js} will be run
 * on the root route as well.
 * <p>
 * Setting {@link #imports()} or {@link #importAll()} changes the meaning of the
 * annotation: instead of loading the module for its side effects, the named
 * values are imported from it and made available to JavaScript expressions sent
 * from the server. See {@link com.vaadin.flow.dom.JsImports} for how to use
 * them. Such an annotation does not have to be on a {@link Component} class; a
 * class whose only purpose is to declare the imports is usually clearer.
 *
 * @author Vaadin Ltd
 * @since 2.0
 *
 * @see CssImport
 * @see com.vaadin.flow.dom.JsImports
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(JsModule.Container.class)
@Inherited
public @interface JsModule {

    /**
     * JavaScript module to load before using the annotated {@link Component} in
     * the browser.
     * <p>
     * NOTE: In the case of using JsModule with LitTemplate, the value needs to
     * point to a real file as it will be copied to the templates folder under
     * target folder. An exported alias from the package will not work.
     *
     * @return a JavaScript module identifier
     */
    String value();

    /**
     * Defines if the JavaScript should be loaded only when running in
     * development mode (for development tooling etc.) or if it should always be
     * loaded.
     * <p>
     * By default, scripts are always loaded.
     *
     * @return {@code true} to load the module only in development mode,
     *         {@code false} to always load it
     * @since 24.2
     */
    boolean developmentOnly() default false;

    /**
     * The names of the values to import from the module and publish for the
     * annotated class, or an empty array to load the module for its side
     * effects only.
     * <p>
     * Each entry must name a single export of the module and be a valid
     * JavaScript identifier; use {@code "default"} for the default export. The
     * imported values are collected into a single object that a JavaScript
     * expression sent from the server can receive as a parameter, see
     * {@link com.vaadin.flow.dom.JsImports}:
     *
     * <pre>
     * &#64;JsModule(value = "lit-html", imports = { "render", "html" })
     * public final class LitImports {
     * }
     *
     * element.executeJs("$0.render($0.html`&lt;div&gt;${$1}&lt;/div&gt;`, this)",
     *         JsImports.of(LitImports.class), "Hello");
     * </pre>
     *
     * A module declaring imports is not added to the page for its side effects,
     * and it is not part of the eager bundle: it is loaded on demand the first
     * time an expression using it is sent to the browser.
     * <p>
     * {@link #developmentOnly()} applies as usual: such a declaration is left
     * out of a production build, so an expression using it must not run in
     * production either.
     * <p>
     * A name may only be declared once per class, so two modules exporting the
     * same name cannot be combined on one class. There is no way to rename an
     * export; declare the modules on separate classes and pass both to
     * {@code executeJs} as separate parameters, or write a JavaScript module
     * that re-exports the value under the wanted name.
     * <p>
     * Only modules that are part of the bundle can be imported by name, since
     * the import is resolved when the bundle is built. Declaring imports from
     * an external URL fails the build; load such a URL at runtime with
     * {@link JavaScript @JavaScript} and {@link JavaScript.Type#MODULE}
     * instead.
     * <p>
     * Cannot be combined with {@link #importAll()} on the same annotation.
     *
     * @return the names to import from the module
     */
    String[] imports() default {};

    /**
     * Whether the whole module namespace should be imported and published for
     * the annotated class instead of individual {@link #imports() names}.
     * <p>
     * The object a JavaScript expression receives is then the module's
     * namespace object, so every export is reachable through it. Since a
     * namespace object would shadow any individually imported value, a class
     * using {@code importAll} must not declare any other imports; declare
     * additional modules on separate classes instead and pass each as its own
     * {@code executeJs} parameter. Setting {@link #imports()} on the same
     * annotation is likewise rejected, as the namespace already contains those
     * values.
     *
     * @return {@code true} to import the whole module namespace
     */
    boolean importAll() default false;

    /**
     * Internal annotation to enable use of multiple {@link JsModule}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @Inherited
    @interface Container {

        /**
         * Internally used to enable use of multiple {@link JsModule}
         * annotations.
         *
         * @return an array of the JavaScript annotations
         */
        JsModule[] value();
    }
}
