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
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Loads style sheets into the browser according to a given loading mode. Style
 * sheet URLs can be either a file served by the application itself, i.e. in
 * public static resource locations like
 * {@code src/main/resources/META-INF/resources/}, or an external URL.
 * <p>
 * Can define style sheet dependencies for a {@link Component} class or
 * globally.
 * <p>
 * When this annotation is placed on the
 * {@link com.vaadin.flow.component.page.AppShellConfigurator}, the referenced
 * style sheets are injected globally into the generated {@code index.html}
 * during bootstrap. In this case the style sheet is always inlined in the body
 * of the html page, thus {@link LoadMode} values are ignored.
 * <p>
 * This annotation is a recommended way to select a Vaadin theme and to import
 * stylesheets. To be put on a class implementing
 * {@link com.vaadin.flow.component.page.AppShellConfigurator} in this case.
 * Example of usage:
 * 
 * <pre>
 *     // theme selection
 *     &#64;StyleSheet(Aura.STYLESHEET) // or Lumo.STYLESHEET
 *     public class Application implements AppShellConfigurator {
 *         ...
 *     }
 *
 *     OR
 *
 *     &#64;StyleSheet("styles.css") // from src/main/resources/META-INF/resources/styles.css
 *     public class Application implements AppShellConfigurator {
 *         ...
 *     }
 *     // and using @import in the styles.css:
 *
 *     &#64;import '@vaadin/aura/aura.css';
 *     // your custom styles ...
 * </pre>
 * <p>
 * For adding multiple style sheets, you can use this annotation multiple times.
 * It is guaranteed that dependencies will be loaded only once.
 * <p>
 * Absolute URLs are used as-is; values prefixed with {@code context://} are
 * resolved using context path (e.g. {@code context://styles.css} becomes
 * {@code /my-app/styles.css}) for context path {@code /my-app}.
 * <p>
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(StyleSheet.Container.class)
public @interface StyleSheet {

    /**
     * Style sheet file URL to load before using the annotated {@link Component}
     * in the browser.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     *
     * @return a style sheet file URL
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
     * Internal annotation to enable use of multiple {@link StyleSheet}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link StyleSheet}
         * annotations.
         *
         * @return an array of the style sheet annotations
         */
        StyleSheet[] value();
    }

}
