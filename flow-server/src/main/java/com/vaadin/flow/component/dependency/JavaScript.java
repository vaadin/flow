/*
 * Copyright 2000-2025 Vaadin Ltd.
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
 * Defines JavaScript dependencies on a {@link Component} class. For adding
 * multiple JavaScript files for a single component, you can use this annotation
 * multiple times.
 * <p>
 * It is guaranteed that dependencies will be loaded only once. The files loaded
 * will be in the same order as the annotations were on the class. However,
 * loading order is only guaranteed on a class level; Annotations from different
 * classes may appear in different order, grouped by the annotated class. Also,
 * files identified by {@code @JavaScript} will be loaded after
 * {@link com.vaadin.flow.component.dependency.JsModule} and before
 * {@link com.vaadin.flow.component.dependency.CssImport}.
 * <p>
 * NOTE: Currently all frontend resources are bundled together into one big
 * bundle. This means, that JavaScript files loaded by one class will be present
 * on a view constructed by another class. For example, if there are two classes
 * {@code RootRoute} annotated with {@code @Route("")}, and another class
 * {@code RouteA} annotated with {@code @Route("route-a")} and
 * {@code @JavaScript("./src/javascript.js")}, the {@code javascript.js} will be
 * run on the root route as well.
 * <p>
 * External JavaScript dependencies (e.g. "http://example.com/some.js") are
 * added in the same way as {@link Page#addJavaScript(String)} and the result is
 * just adding a classic {@code javscript} element to the page. Other paths used
 * in the {@link JavaScript#value()} method are considered as relative to
 * {@code frontend} directory and they are added to the page as a JavaScript
 * module (a {@code javscript} element with {@code type="module"}). In this case
 * a {@link JavaScript} annotation behaves exactly as a {@link JsModule}
 * annotation.
 * <p>
 * It's not possible to execute a function defined in JavaScript module via
 *
 * <pre>
 *
 * <code>
 * UI.getCurrent().getPage().executeJs("some_function_definied_in_module()");
 * </code>
 * </pre>
 *
 * because the function is private there (unless it's explicitly exposed). The
 * JavaScript where the function is defined should be either external or it
 * should be added using {@link Page#addJavaScript(String)}: in this case all
 * declared functions become available in the global scope.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see Page#addJavaScript(String)
 * @see JsModule
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(JavaScript.Container.class)
public @interface JavaScript {

    /**
     * JavaScript file URL to load before using the annotated {@link Component}
     * in the browser.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location.
     * <p>
     * This URL identifies a file which will be bundled, so the file should be
     * available to be able to bundle it.
     * <p>
     * You can prefix the URL with {@code context://} to make it relative to the
     * context path or use an absolute URL to refer to files outside the
     * frontend directory. Such URLs are not bundled but included into the page
     * as standalone scripts in the same way as it's done by
     * {@link Page#addJavaScript(String)}.
     *
     * @return a JavaScript file URL
     */
    String value();

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
