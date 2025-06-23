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
package com.vaadin.flow.theme;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the theme to use for the application. Should be present only once for
 * an app, in the `AppShellConfiguration` class.
 * <p>
 * The {@code value} property defines the name of the application theme. When
 * the theme is present inside the project, it maps to the {@code
 * frontend/themes/&#60application-theme-name&#62}/ folder or alternatively to a
 * folder inside the static resources of a jar file, like {@code
 * src/main/resources/META-INF/resources/themes/&#60application-theme-name&#62/}
 * . The application theme is always based on Lumo theme and this is the
 * recommended way to theme applications starting from Flow 6.0 and Vaadin 19.
 * <p>
 * Alternatively , if instead of Lumo theme the Material theme or another "old
 * style custom theme" is to be used, that can be specified with the {@code
 * themeClass} property. This allows defining a way to translate base component
 * url to the themed component url (@see {@link AbstractTheme}), which specifies
 * components styles.
 * <p>
 * By default {@code com.vaadin.flow.theme.lumo.Lumo} theme is used if it's in
 * the classpath. You may disable theming with {@link NoTheme} annotation.
 * <p>
 * {@link Theme} annotation should be added to the AppShellConfigurator
 * implementation.
 *
 * <p>
 * Only a single theme can be defined and having multiple instances will throw
 * an exception.
 *
 * <p>
 * Here is some examples:
 *
 * <pre>
 * <code>
 *
 * &#64;Theme("my-theme")
 * public class MyAppShell implements AppShellConfigurator {
 * }
 * </code>
 * </pre>
 *
 * <pre>
 * <code>
 *
 * &#64;Theme(themeClass = Lumo.class)
 * public class MyAppShell implements AppShellConfigurator {
 * }
 * </code>
 * </pre>
 *
 * @author Vaadin Ltd
 * @see AbstractTheme
 * @see NoTheme
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
     * @return theme handler
     */
    Class<? extends AbstractTheme> themeClass() default AbstractTheme.class;

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
    String value() default "";
}
