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
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Loads a style sheet into the browser at runtime as a
 * {@code <link rel="stylesheet">} element. The referenced file is served as a
 * static resource by the servlet container; it is not bundled. Use
 * {@link CssImport} when the CSS file should be processed as a bundle source by
 * Vite at build time.
 * <p>
 * Source locations:
 * <ul>
 * <li>Application projects: {@code src/main/resources/META-INF/resources/}. In
 * Spring Boot projects {@code src/main/resources/public/} or
 * {@code src/main/resources/static/} are also served.</li>
 * <li>Add-on JARs: {@code META-INF/resources/}. Files placed there are served
 * automatically by the servlet container when the JAR is on the classpath.</li>
 * </ul>
 * <p>
 * URL resolution is identical whether the annotation is placed on an
 * {@link com.vaadin.flow.component.page.AppShellConfigurator}, a route
 * component, or any other {@link Component}. See {@link #value()} for the
 * resolution rules.
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
 * NOTE: while this annotation is not inherited using the
 * {@link Inherited @Inherited} annotation, the annotations of the possible
 * parent components or implemented interfaces are read when sending the
 * dependencies to the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @see CssImport
 * @see JavaScript
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
     * URL resolution rules, in order:
     * <ul>
     * <li>{@code http://...}, {@code https://...}, {@code //...} — used
     * unchanged.</li>
     * <li>{@code context://foo.css} — resolved against the servlet context root
     * (independent of the Vaadin servlet mapping).</li>
     * <li>{@code base://foo.css} — resolved against the page's {@code <base>}
     * URI, i.e. the Vaadin servlet mapping path. Use this as an explicit opt-in
     * for servlet-mapping-relative loading.</li>
     * <li>{@code /foo.css} — used unchanged as an absolute server path.</li>
     * <li>{@code ./foo.css} — leading {@code ./} is stripped, then treated as
     * context-root relative.</li>
     * <li>{@code foo.css}, {@code styles/foo.css} — context-root relative, i.e.
     * equivalent to {@code context://foo.css}.</li>
     * <li>Values containing {@code ..} (path traversal) are rejected with a
     * warning.</li>
     * </ul>
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
