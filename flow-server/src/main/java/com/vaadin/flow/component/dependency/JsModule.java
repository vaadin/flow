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
 * Defines a JavaScript module dependency on a {@link Component} class.
 * <p>
 * This is a <em>build-time</em> dependency: the referenced file is a bundle
 * source, fed into Vite at build time, and is not served as a static resource
 * at runtime. Use {@link JavaScript} for files that should be loaded at runtime
 * (including external URLs and CDN-hosted scripts).
 * <p>
 * Source locations:
 * <ul>
 * <li>Application projects: {@code src/main/frontend/} (recommended), or the
 * legacy top-level {@code frontend/} directory.</li>
 * <li>Add-on JARs: {@code META-INF/frontend/} (recommended). The legacy
 * location {@code META-INF/resources/frontend/} is still supported but
 * deprecated and emits a build-time warning.</li>
 * </ul>
 * <p>
 * For adding multiple JavaScript module files for a single component, use this
 * annotation multiple times.
 * <p>
 * It is guaranteed that dependencies will be loaded only once. Loading order is
 * guaranteed at the class level (annotations on different classes may appear in
 * different orders relative to each other). {@code @JsModule} files load before
 * {@link JavaScript} and {@link CssImport}.
 * <p>
 * Runtime URLs (with a scheme such as {@code https://}, {@code context://} or
 * {@code base://}, protocol-relative URLs ({@code //}) and values with a
 * leading {@code /}) are deprecated. On a component, an interface or the app
 * shell, they are not bundled but loaded at runtime as
 * {@code <script type="module">}. On any other class, they are not loaded at
 * runtime; a value with a leading {@code /} is still bundled from the frontend
 * folder, and the frontend build fails if the file is not there. Use
 * {@link JavaScript} with {@link JavaScript.Type#MODULE} for such files
 * instead. See {@link #value()}.
 * <p>
 * NOTE: Currently all frontend resources are bundled together into one big
 * bundle. JavaScript files loaded by one class will therefore be present on a
 * view constructed by another class.
 *
 * @author Vaadin Ltd
 * @since 2.0
 *
 * @see CssImport
 * @see JavaScript
 * @see StyleSheet
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
     * The recommended value is a bundler import specifier — typically a
     * relative path (e.g. {@code "./my-element.js"}), an npm package specifier
     * (e.g. {@code "@scope/pkg/foo.js"}), or an alias. The bundler resolves
     * these against the configured frontend directory.
     * <p>
     * Using a runtime URL (one starting with {@code context://},
     * {@code base://}, {@code /}, {@code http://}, {@code https://} or
     * {@code //}, or any other URL containing {@code ://}) is deprecated: on a
     * component, an interface (such as a mixin implemented by components) or
     * the app shell, such a value is not bundled but loaded at runtime as a
     * {@code <script type="module">}, and a warning is logged during the
     * production build. As with {@link JavaScript}, a value with a leading
     * {@code /} (including a protocol-relative {@code //} URL) that contains a
     * path traversal ({@code ..}) is rejected with a warning and not loaded at
     * all; values with a scheme, including {@code context://} and
     * {@code base://}, are passed on unchanged. On any other class, such as a
     * {@code VaadinServiceInitListener}, nothing loads the value at runtime: a
     * URL is not loaded at all, and a value with a leading {@code /} is still
     * bundled from the frontend folder, which makes the frontend build fail if
     * the file is not there. Use {@link JavaScript @JavaScript} with
     * {@link JavaScript.Type#MODULE} on a component or the app shell for
     * runtime modules instead. To bundle a file in the frontend folder, use a
     * relative value such as {@code ./foo.js}.
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
