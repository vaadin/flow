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
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Loads a JavaScript file into the browser at runtime. By default the file is
 * loaded as a classic {@code <script>} element; set {@link #type()} to
 * {@link Type#MODULE} to load it as a {@code <script type="module">} element
 * instead. The referenced file is served as a static resource by the servlet
 * container; it is not bundled. Use {@link JsModule} when the file should be
 * processed as a bundle source by Vite at build time.
 * <p>
 * Source locations (same as {@link StyleSheet}):
 * <ul>
 * <li>Application projects: {@code src/main/resources/META-INF/resources/}. In
 * Spring Boot projects {@code src/main/resources/public/} or
 * {@code src/main/resources/static/} are also served.</li>
 * <li>Add-on JARs: {@code META-INF/resources/}.</li>
 * </ul>
 * <p>
 * URL resolution follows the same rules as {@link StyleSheet#value()} —
 * {@code context://}, {@code base://}, absolute URLs, and bare relative paths
 * are all supported. Use {@code context://foo.js} to load
 * {@code META-INF/resources/foo.js} regardless of the Vaadin servlet mapping.
 * External URLs ({@code https://cdn.example/foo.js}) work the same way.
 * <p>
 * For adding multiple JavaScript files for a single component, use this
 * annotation multiple times. It is guaranteed that dependencies will be loaded
 * only once.
 * <p>
 * <b>Deprecated bundled interpretation:</b> Historically, a bare relative URL
 * (e.g. {@code @JavaScript("./foo.js")}) was collected by the scanner and
 * bundled like {@link JsModule}. This behavior is deprecated when
 * {@link #type()} is the default {@link Type#SCRIPT}. To opt into runtime
 * semantics, either prefix the value with {@code context://}, {@code base://},
 * or {@code /}, or set {@link #type()} to {@link Type#MODULE}. Bare-relative
 * SCRIPT-typed values still bundle but emit a build-time warning. A future
 * major release will flip the default so bare relatives resolve against the
 * context root.
 * <p>
 * NOTE: When loaded with {@link Type#MODULE} (or via {@link JsModule}), it is
 * not possible to execute a function defined in the file via
 *
 * <pre>
 *
 * <code>
 * UI.getCurrent().getPage().executeJs("some_function_definied_in_module()");
 * </code>
 * </pre>
 *
 * because the function is private to the module (unless it's explicitly
 * exposed). The JavaScript where the function is defined should be loaded with
 * {@link Type#SCRIPT} (the default) or added using
 * {@link Page#addJavaScript(String)}: in this case all declared functions
 * become available in the global scope.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see Page#addJavaScript(String)
 * @see JsModule
 * @see StyleSheet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(JavaScript.Container.class)
public @interface JavaScript {

    /**
     * The kind of {@code <script>} tag to render for the dependency.
     */
    enum Type {
        /**
         * Render a classic {@code <script>} tag. Functions declared in the
         * loaded file become available in the global scope.
         */
        SCRIPT,
        /**
         * Render a {@code <script type="module">} tag. The loaded file is
         * treated as an ES module: functions and variables declared in it are
         * private to the module unless explicitly exported. The file is loaded
         * at runtime and is not bundled, even when the URL is a bare relative
         * path. Use this for hand-authored or CDN-hosted modules that should
         * not go through Vite. For build-time bundled ES modules use
         * {@link JsModule} instead.
         */
        MODULE
    }

    /**
     * JavaScript file URL to load before using the annotated {@link Component}
     * in the browser.
     * <p>
     * URL resolution rules (same as {@link StyleSheet#value()}), in order:
     * <ul>
     * <li>{@code http://...}, {@code https://...}, {@code //...} — used
     * unchanged.</li>
     * <li>{@code context://foo.js} — resolved against the servlet context root
     * (independent of the Vaadin servlet mapping).</li>
     * <li>{@code base://foo.js} — resolved against the page's {@code <base>}
     * URI, i.e. the Vaadin servlet mapping path.</li>
     * <li>{@code /foo.js} — used unchanged as an absolute server path.</li>
     * <li>Any other value (bare relative, {@code "./foo.js"},
     * {@code "../foo.js"}) is currently treated as a bundle source for
     * backwards compatibility — see the deprecation notice on the class
     * Javadoc. Migrate to {@code @JavaScript("context://foo.js")} for runtime
     * loading or to {@link JsModule} for bundling.</li>
     * </ul>
     * <p>
     * When {@link #type()} is {@link Type#MODULE}, the value is loaded at
     * runtime regardless of whether it has a URL prefix; bare relative paths
     * are normalized to {@code context://<value>} and served as static
     * resources by the servlet container.
     *
     * @return a JavaScript file URL
     */
    String value();

    /**
     * The kind of {@code <script>} tag to use when loading the file. Defaults
     * to {@link Type#SCRIPT} (a classic {@code <script>} element). Set to
     * {@link Type#MODULE} to render a {@code <script type="module">} element
     * instead, e.g. for hand-authored or CDN-hosted ES modules that should not
     * go through Vite. For build-time bundled ES modules use {@link JsModule}
     * instead.
     *
     * @return the kind of script tag to render
     */
    Type type() default Type.SCRIPT;

    /**
     * Defines if the JavaScript should be loaded only when running in
     * development mode (for development tooling etc.) or if it should always be
     * loaded.
     * <p>
     * By default, scripts are always loaded.
     *
     * @return {@code true} to load the script only in development mode,
     *         {@code false} to always load it
     */
    boolean developmentOnly() default false;

    /**
     * Determines the dependency load mode. Refer to {@link LoadMode} for the
     * details.
     *
     * @return load mode for the dependency
     */
    LoadMode loadMode() default LoadMode.EAGER;

    /**
     * Internal annotation to enable use of multiple {@link JavaScript}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link JavaScript}
         * annotations.
         *
         * @return an array of the JavaScript annotations
         */
        JavaScript[] value();
    }

}
