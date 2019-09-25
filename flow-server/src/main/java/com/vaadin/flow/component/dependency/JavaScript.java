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
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 * <p>
 * NOTE: Currently all frontend resources are bundled together into one big
 * bundle. This means, that JavaScript files loaded by one class will be present
 * on a view constructed by another class. For example, if there are two classes
 * {@code RootRoute} annotated with {@code @Route("")}, and another class
 * {@code RouteA} annotated with {@code @Route("route-a")} and
 * {@code @JavaScript("./src/javascript.js")}, the {@code javascript.js} will be
 * run on the root route as well.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     *
     * @return a JavaScript file URL
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
