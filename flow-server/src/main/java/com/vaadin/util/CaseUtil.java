/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.util;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities related to various case operations.
 *
 * @author Vaadin Ltd
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
     *            The input string in UPPER_CASE_UNDERSCORE format, not
     *            {@code null}
     * @return A human friendly version of the input
     */
    public static String upperCaseUnderscoreToHumanFriendly(
            String upperCaseUnderscoreString) {
        String[] parts = upperCaseUnderscoreString.replaceFirst("^_*", "")
                .split("_");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = StringUtils
                    .capitalize(parts[i].toLowerCase(Locale.ROOT));
        }
        return String.join(" ", parts);
    }

}
