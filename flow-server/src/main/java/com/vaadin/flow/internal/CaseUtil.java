/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Locale;

/**
 * Utilities related to various case operations.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
