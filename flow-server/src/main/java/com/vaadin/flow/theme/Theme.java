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

/**
 * Defines that there is a theme to use and defines the theme handler
 * implementation.
 * <p>
 * The theme allows to define a way to translate base component url to the
 * themed component url (@see {@link AbstractTheme}), which specifies components
 * styles.
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
 * Here is an example:
 *
 * <pre>
 * <code>
 *
 * &#64;Theme(Lumo.class)
 * public class MyAppShell implements AppShellConfigurator {
 * }
 * </code>
 * </pre>
 *
 * @see AbstractTheme
 * @see NoTheme
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
     * @return theme handler
     */
    Class<? extends AbstractTheme> value();

    /**
     * The theme variant, if any.
     *
     * @return the theme variant
     */
    String variant() default "";
}
