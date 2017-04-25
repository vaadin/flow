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

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for validating custom-element name according to definition in <a href=
 * "https://www.w3.org/TR/custom-elements/#prod-potentialcustomelementname">Custom
 * element name</a>
 */
public final class CustomElementNameValidator {

    private static Set<String> reservedNames = Stream.of("annotation-xml",
            "color-profile", "font-face", "font-face-src", "font-face-uri",
            "font-face-format", "font-face-name", "missing-glyph")
            .collect(Collectors.toSet());

    // https://html.spec.whatwg.org/multipage/scripting.html#prod-potentialcustomelementname
    private static String customElementRegex = "^[a-z](?:[\\-\\.0-9_a-z\\xB7\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u037D\\u037F-\\u1FFF\\u200C\\u200D\\u203F\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|[\\uD800-\\uDB7F][\\uDC00-\\uDFFF])*-(?:[\\-\\.0-9_a-z\\xB7\\xC0-\\xD6\\xD8-\\xF6\\xF8-\\u037D\\u037F-\\u1FFF\\u200C\\u200D\\u203F\\u2040\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|[\\uD800-\\uDB7F][\\uDC00-\\uDFFF])*";

    private CustomElementNameValidator() {
    }

    /**
     * Validate that given name is a valid Custom Element name.
     * 
     * @param name
     *            Name to validate
     * @return Result containing possible validation error and/or warning
     */
    public static Result validate(String name) {
        return new Result(hasError(name), hasWarning(name));
    }

    private static String hasError(String name) {
        String result = "";
        if (name == null || name.isEmpty()) {
            result = "Missing element name.";
        } else if (!name.equals(name.toLowerCase())) {
            result = "Custom element names must not contain uppercase ASCII characters.";
        } else if (!name.contains("-")) {
            result = "Custom element names must contain a hyphen. Example: unicorn-cake";
        } else if (name.matches("\\d.*")) {
            result = "Custom element names must not start with a digit.";
        } else if (name.startsWith("-")) {
            result = "Custom element names must not start with a hyphen.";
        } else if (!name.matches(customElementRegex)) {
            result = "Invalid element name.";
        } else if (reservedNames.contains(name)) {
            result = "The supplied element name is reserved and can\"t be used.\nSee: https://html.spec.whatwg.org/multipage/scripting.html#valid-custom-element-name";
        }
        return result;
    }

    private static String hasWarning(String name) {
        String result = "";
        if (name == null || name.isEmpty()) {
            result = "Missing element name.";
        } else if (name.startsWith("polymer-")) {
            result = "Custom element names should not start with `polymer-`.\nSee: http://webcomponents.github.io/articles/how-should-i-name-my-element";
        } else if (name.startsWith("x-")) {
            result = "Custom element names should not start with `x-`.\nSee: http://webcomponents.github.io/articles/how-should-i-name-my-element/";
        } else if (name.startsWith("ng-")) {
            result = "Custom element names should not start with `ng-`.\nSee: http://docs.angularjs.org/guide/directive#creating-directives";
        } else if (name.startsWith("xml")) {
            result = "Custom element names should not start with `xml`.";
        } else if (Pattern.compile("^[^a-z]").matcher(name).find()) {
            result = "This element name is only valid in XHTML, not in HTML. First character should be in the range a-z.";
        } else if (Pattern.compile("-$").matcher(name).find()) {
            result = "Custom element names should not end with a hyphen.";
        } else if (Pattern.compile("[\\.]").matcher(name).find()) {
            result = "Custom element names should not contain a dot character as it would need to be escaped in a CSS selector.";
        } else if (Pattern.compile("[^\\x20-\\x7E]").matcher(name).find()) {
            result = "Custom element names should not contain non-ASCII characters.";
        } else if (Pattern.compile("--").matcher(name).find()) {
            result = "Custom element names should not contain consecutive hyphens.";
        } else if (Pattern.compile("[^a-z0-9]{2}").matcher(name).find()) {
            result = "Custom element names should not contain consecutive non-alpha characters.";
        }
        return result;
    }

    /**
     * Validation result class that contains information if valid and possible
     * error and/or warning message received during validation.
     */
    public static class Result {
        private String error = "";
        private String warning = "";

        /**
         * Constructor with error and warning resolution.
         * 
         * @param error
         *            Error message
         * @param warning
         *            Warning message
         */
        protected Result(String error, String warning) {
            assert error != null;
            assert warning != null;

            this.error = error;
            this.warning = warning;
        }

        /**
         * Get the error message for this result.
         *
         * @return error message
         */
        public String getError() {
            return error;
        }

        /**
         * Get the result warning.
         * 
         * @return warning message
         */
        public String getWarning() {
            return warning;
        }

        /**
         * Get if result is valid or not.
         * 
         * @return true for valid
         */
        public boolean isValid() {
            return error.isEmpty();
        }
    }
}
