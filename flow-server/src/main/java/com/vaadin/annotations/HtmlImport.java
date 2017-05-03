/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * Annotation for defining HTML dependencies on a {@link Component} class. For
 * adding multiple HTML files for a single component, you can use this
 * annotation multiple times.
 * <p>
 * It is guaranteed that all blocking HTML files are loaded on the client side
 * before the component is used for the first time in a {@link UI}. Non-blocking
 * style sheet files can be loaded later, for more details refer to
 * {@link #blocking()}. The dependencies are sent only once.
 * <p>
 * By default, the resource is loaded in blocking mode, that assumes that an
 * application cannot work without having the it loaded first.
 * To change this behaviour, please specify {@link #blocking()} property.
 * <p>
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(InternalContainerAnnotationForHtml.class)
public @interface HtmlImport {

    /**
     * HTML file URL to load before using the annotated {@link Component} in the
     * browser.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     * <p>
     * When using compiled web components, you can prefix the URL with
     * {@literal frontend://} to serve different files to different browsers,
     * based on their ES6 support. For example, when using
     * {@literal frontend://MyComponent.html}, the evaluated URL will be:
     * <ul>
     * <li>{@literal context://VAADIN/static/frontend/es6/MyComponent.html} for
     * ES6 capable browsers;</li>
     * <li>{@literal context://VAADIN/static/frontend/es5/MyComponent.html} for
     * other browsers.</li>
     * </ul>
     *
     * @return a html file URL
     * @see WebComponents
     */
    String value();

    /**
     * Indicates whether resource should be loaded before component is used for
     * the first time or not.
     * <p>
     * Component can be used only if all corresponding blocking resources are
     * loaded.
     *
     * @return {@code false} if the resource can be loaded before component
     *         usage, {@code true} otherwise
     */
    boolean blocking() default true;
}
