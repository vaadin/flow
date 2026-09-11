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
package com.vaadin.flow.theme;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation to avoid applying any default theme to the application.
 *
 * <p>
 * Flow uses the following logic to determine which theme to use for the
 * application:
 * <ul>
 * <li>If a {@link Theme} annotation is found on the AppShellConfigurator, the
 * theme defined by it is used.
 * <li>If a {@link NoTheme} annotation is found on the AppShellConfigurator,
 * theming is disabled.
 * <li>If the <code>com.vaadin.flow.theme.lumo.Lumo</code> class is available in
 * the classpath (which comes from the vaadin-lumo-theme project), then it is
 * used as the default theme.
 * </ul>
 * <p>
 * {@link NoTheme} annotation should be added to the AppShellConfigurator
 * implementation.
 *
 * @see Theme
 * @since 1.0
 * @deprecated As of Vaadin 25, this annotation is deprecated together with
 *             {@link Theme}, since the theming system has been reworked and no
 *             theme is applied to the application unless it is explicitly
 *             requested. Omitting {@link Theme} has the same effect as using
 *             this annotation, so it can simply be removed. Styles are now
 *             loaded with
 *             {@link com.vaadin.flow.component.dependency.StyleSheet} from
 *             public static resources locations or
 *             {@link com.vaadin.flow.component.dependency.CssImport} from a
 *             {@code src/main/frontend/} folder, together with mechanisms
 *             native to HTML, CSS and React (e.g.
 *             {@code @import url("morestyles.css")} in CSS).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Deprecated(since = "25.1")
public @interface NoTheme {
}
