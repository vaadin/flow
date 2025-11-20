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
package com.vaadin.flow.component.page;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the color scheme for the application using the CSS color-scheme
 * property.
 * <p>
 * This annotation should be placed on a class that implements
 * {@link com.vaadin.flow.component.page.AppShellConfigurator} to set the
 * initial color scheme for the entire application.
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;ColorScheme(ColorScheme.Value.DARK)
 * public class AppShell implements AppShellConfigurator {
 * }
 * </pre>
 * <p>
 * The color scheme can also be changed programmatically at runtime using
 * {@link Page#setColorScheme(ColorScheme.Value)}.
 *
 * @see Page#setColorScheme(ColorScheme.Value)
 * @see Page#getColorScheme()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ColorScheme {

    /**
     * The initial color scheme for the application.
     *
     * @return the color scheme value
     */
    Value value() default Value.NORMAL;

    /**
     * Enumeration of supported color scheme values.
     * <p>
     * These values correspond to the CSS color-scheme property values and
     * control how the browser renders UI elements and how the application
     * responds to system color scheme preferences.
     */
    enum Value {
        /**
         * Light color scheme only. The application will use a light theme
         * regardless of system preferences.
         */
        LIGHT("light"),

        /**
         * Dark color scheme only. The application will use a dark theme
         * regardless of system preferences.
         */
        DARK("dark"),

        /**
         * Supports both light and dark color schemes, with a preference for
         * light. The application can adapt to system preferences but defaults
         * to light mode.
         */
        LIGHT_DARK("light dark"),

        /**
         * Supports both light and dark color schemes, with a preference for
         * dark. The application can adapt to system preferences but defaults to
         * dark mode.
         */
        DARK_LIGHT("dark light"),

        /**
         * Normal/default color scheme. Uses the browser's default behavior
         * without any specific color scheme preference.
         */
        NORMAL("normal");

        private final String value;

        Value(String value) {
            this.value = value;
        }

        /**
         * Gets the CSS color-scheme property value.
         *
         * @return the CSS value string
         */
        public String getValue() {
            return value;
        }

        /**
         * Converts a string to a ColorScheme.Value enum.
         *
         * @param value
         *            the CSS color-scheme value string
         * @return the corresponding enum value, or NORMAL if not recognized
         */
        public static Value fromString(String value) {
            if (value == null || value.isEmpty()) {
                return NORMAL;
            }
            for (Value v : values()) {
                if (v.value.equals(value)) {
                    return v;
                }
            }
            return NORMAL;
        }
    }
}
