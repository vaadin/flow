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
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates custom-element name according to definition in <a href=
 * "https://www.w3.org/TR/custom-elements/#prod-potentialcustomelementname">Custom
 * element name</a>
 *
 * @since 1.0
 */
public final class CustomElementNameValidator {
    private static final Pattern STARTS_WITH_A_DIGIT = Pattern.compile("\\d.*");

    private static final Set<String> RESERVED_NAMES = Stream
            .of("annotation-xml", "color-profile", "font-face", "font-face-src",
                    "font-face-uri", "font-face-format", "font-face-name",
                    "missing-glyph")
            .collect(Collectors.toSet());

    // https://html.spe
    // nbc.whatwg.org/multipage/scripting.html#prod-potentialcustomelementname
    private static final String CUSTOM_ELEMENT_REGEX = "^[a-z](?:[\\-\\.0-9_a-z\\xB7\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u037D\\u037F-\\u1FFF\\u200C\\u200D\\u203F\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|[\\uD800-\\uDB7F][\\uDC00-\\uDFFF])*-(?:[\\-\\.0-9_a-z\\xB7\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u037D\\u037F-\\u1FFF\\u200C\\u200D\\u203F\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|[\\uD800-\\uDB7F][\\uDC00-\\uDFFF])*";
    private static final Pattern WEB_COMPONENT_COMPLIANT_NAME_REGEX = Pattern
            .compile(CUSTOM_ELEMENT_REGEX);

    /**
     * Validate that given name is a valid Custom Element name.
     *
     * @param name
     *            Name to validate
     * @return Result containing possible validation error and/or warning
     */
    public static boolean isCustomElementName(String name) {
        return name != null && checkHtmlTagRules(name)
                && checkWebComponentRules(name);
    }

    private static boolean checkHtmlTagRules(String name) {
        return !name.isEmpty() && name.equals(name.toLowerCase(Locale.ENGLISH))
                && !name.startsWith("-")
                && !STARTS_WITH_A_DIGIT.matcher(name).matches();
    }

    private static boolean checkWebComponentRules(String name) {
        return name.contains("-") && !RESERVED_NAMES.contains(name)
                && WEB_COMPONENT_COMPLIANT_NAME_REGEX.matcher(name).matches();
    }
}
