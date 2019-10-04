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
 * <li>If a {@link Theme} annotation is found at the root navigation level, the
 * theme defined by it is used.
 * <li>If a {@link NoTheme} annotation is found at the root navigation level,
 * theming is disabled.
 * <li>If the <code>com.vaadin.flow.theme.lumo.Lumo</code> class is available in
 * the classpath (which comes from the vaadin-lumo-theme project), then it is
 * used as the default theme.
 * </ul>
 *
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NoTheme {
}
