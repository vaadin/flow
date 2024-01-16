/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.theme;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

/**
 * Defines that there is a theme to use and defines the theme handler
 * implementation.
 * <p>
 * {@link Theme} annotation should be added to your root navigation level,
 * {@link RouterLayout} or to the top level @{@link Route}.
 * <p>
 * The {@code themeFolder} property defines the name of the application theme.
 * When the theme is present inside the project, it maps to the {@code
 * frontend/theme/<application-theme-name>}/ folder or alternatively to a folder
 * inside the static resources of a jar file, like {@code
 * src/main/resources/META-INF/resources/theme/<application-theme-name>/}. The
 * application theme is always based on Lumo theme and this is the recommended
 * way to theme applications starting from Flow 2.6 and Vaadin 14.6
 * <p>
 * Alternatively , if instead of Lumo theme the Material theme or another "old
 * style custom theme" is to be used, that can be specified with the {@code
 * value} property. This allows defining a way to translate base component url
 * to the themed component url (@see {@link AbstractTheme}), which specifies
 * components styles.
 * <p>
 * By default {@code com.vaadin.flow.theme.lumo.Lumo} theme is used if it's in
 * the classpath. You may disable theming with {@link NoTheme} annotation.
 * Adding an empty {@link Theme} is equal to not having a {@link Theme}
 * annotation and will default to {@code com.vaadin.flow.theme.lumo.Lumo} if
 * available.
 * <p>
 * Defining different Themes for different views will end throwing an exception.
 *
 * <p>
 * Here are examples:
 *
 * <ul>
 * <li>On the navigation root
 *
 * <pre>
 * <code>
 * &#64;Route(value = "")
 * &#64;Theme(Lumo.class)
 * public class Main extends Div {
 * }
 * </code>
 * </pre>
 *
 * <li>on the top level router layout
 *
 * <pre>
 * <code>
 * &#64;Theme(MyTheme.class)
 * public class MainLayout extends Div implements RouterLayout {
 * }
 *
 * &#64;Route(value = "editor", layout = MainLayout.class)
 * public class Editor extends Div {
 * }
 * </code>
 * </pre>
 *
 * </ul>
 *
 * @see AbstractTheme
 * @see NoTheme
 * @see RouterLayout
 * @see Route
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Theme {

    /**
     * The theme translation handler.
     *
     * Defaults to Lumo, If not specified.
     *
     * @return theme handler
     */
    Class<? extends AbstractTheme> value() default AbstractTheme.class;

    /**
     * The theme variant, if any.
     *
     * @return the theme variant
     */
    String variant() default "";

    /**
     * The name of the theme to use.
     *
     * If this is not specified will default to Lumo.
     */
    String themeFolder() default "";
}
