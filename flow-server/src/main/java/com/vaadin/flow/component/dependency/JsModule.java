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
 * It is guaranteed that dependencies will be loaded only once.
 * <p>
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 *
 * @see CssImport
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(JsModule.Container.class)
public @interface JsModule {

    /**
     * JavaScript module to load before using the annotated {@link Component} in
     * the browser.
     *
     * @return a JavaScript module identifier
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
     * Internal annotation to enable use of multiple {@link JsModule}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
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
