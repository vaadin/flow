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
 * <b>Deprecated runtime URLs:</b> Historically, {@code @JsModule} also accepted
 * runtime URLs ({@code http://}, {@code https://}, {@code //},
 * {@code context://}, {@code base://}, {@code /…}); such values were loaded at
 * runtime as {@code <script type="module">}. This is deprecated. Use
 * {@link JavaScript} for runtime script loading. Existing runtime URLs in
 * {@code @JsModule} keep working for backwards compatibility but are excluded
 * from the bundle and emit a build-time warning.
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
     * Values with a runtime URL prefix ({@code http://}, {@code https://},
     * {@code //}, {@code context://}, {@code base://}, or a leading {@code /})
     * are deprecated; they are excluded from the bundle and emit a build-time
     * warning. Migrate them to {@link JavaScript}.
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
