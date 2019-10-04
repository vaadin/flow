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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Defines HTML dependencies on a {@link Component} class. For adding multiple
 * HTML files for a single component, you can use this annotation multiple
 * times.
 * <p>
 * It is guaranteed that dependencies will be loaded only once.
 * </p>
 * <p>
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 * </p>
 * <p>
 * NOTE: this annotation is only useful in compatibility mode and it is ignored
 * in normal mode. In order to use a Polymer template inside a component in
 * Vaadin 14+, {@link JsModule} annotation should be used. And to use a css
 * file, {@link CssImport} should be used. If you want to be able to use your
 * component in both compatibility mode and normal mode of Vaadin 14+ you need
 * to have @HtmlImport along with @JsModule and/or @CssImport annotations. See
 * <a href="https://vaadin.com/docs/v14/flow/v14-migration/v14-migration-guide.html#3-convert-polymer-2-to-polymer-3">Vaadin 14 Migration Guide</a>
 * for more information on how to migrate templates from Polymer 2 to Polymer 3.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(HtmlImport.Container.class)
public @interface HtmlImport {

    /**
     * HTML file URL to load before using the annotated {@link Component} in the
     * browser.
     * <p>
     * When using compiled web components, you can use a relative URL or prefix
     * the URL with {@code frontend://} to serve different files to different
     * browsers, based on their ES6 support. For example, when using
     * {@code "MyComponent.html"}, the evaluated URL will be:
     * <ul>
     * <li>{@code context://frontend/MyComponent.html} during development;</li>
     * <li>{@code context://frontend-es6/MyComponent.html} for ES6 capable
     * browsers;</li>
     * <li>{@code context://frontend-es5/MyComponent.html} for other
     * browsers.</li>
     * </ul>
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     *
     * @return a html file URL
     */
    String value();

    /**
     * Determines the dependency load mode. Refer to {@link LoadMode} for the
     * details.
     *
     * @return load mode for the dependency
     */
    LoadMode loadMode() default LoadMode.EAGER;

    /**
     * Internal annotation to enable use of multiple {@link HtmlImport}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link HtmlImport}
         * annotations.
         *
         *
         * @return an array of the HtmlImport annotations
         */
        HtmlImport[] value();
    }
}
