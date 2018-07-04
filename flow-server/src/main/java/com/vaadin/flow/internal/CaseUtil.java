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
package com.vaadin.flow.internal;

import java.util.Locale;

/**
 * Utilities related to various case operations.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class CaseUtil {

    private CaseUtil() {
        // Static utils only
    }

    /**
     * Converts an UPPER_CASE_STRING to a human friendly format (Upper Case
     * String).
     * <p>
     * Splits words on {@code _}. Examples:
     * <p>
     * {@literal MY_BEAN_CONTAINER} becomes {@literal My Bean Container}
     * {@literal AWESOME_URL_FACTORY} becomes {@literal Awesome Url Factory}
     * {@literal SOMETHING} becomes {@literal Something}
     *
     * @param upperCaseUnderscoreString
     *            The input string in UPPER_CASE_UNDERSCORE format
     * @return A human friendly version of the input
     */
    public static String upperCaseUnderscoreToHumanFriendly(
            String upperCaseUnderscoreString) {
        if (upperCaseUnderscoreString == null) {
            return null;
        }
        String[] parts = upperCaseUnderscoreString.replaceFirst("^_*", "")
                .split("_");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = capitalize(parts[i].toLowerCase(Locale.ROOT));
        }
        return String.join(" ", parts);
    }

    /**
     * Capitalizes the first character in the given string in a way suitable for
     * use in code (methods, properties etc).
     *
     * @param string
     *            The string to capitalize
     * @return The capitalized string
     */
    public static String capitalize(String string) {
        if (string == null) {
            return null;
        }

        if (string.length() <= 1) {
            return string.toUpperCase(Locale.ROOT);
        }

        return string.substring(0, 1).toUpperCase(Locale.ROOT)
                + string.substring(1);
    }

}
