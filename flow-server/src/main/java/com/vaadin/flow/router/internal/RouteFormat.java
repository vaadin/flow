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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Set;

import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameterRegex;

/**
 * Utility class which contains various methods for defining url parameter
 * template.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class RouteFormat implements Serializable {

    static final String INTEGER_REGEX = RouteParameterRegex.INTEGER;
    static final String LONG_REGEX = RouteParameterRegex.LONG;
    static final String BOOLEAN_REGEX = RouteParameterRegex.BOOLEAN;
    static final String STRING_REGEX = "";

    /**
     * Returns whether the specified template contains route parameters.
     *
     * @param template
     *            a template.
     * @return true if the specified template contains route parameters,
     *         otherwise false.
     */
    static boolean hasParameters(String template) {
        return template.contains(":");
    }

    /**
     * Returns whether the specified template contains route parameters.
     *
     * @param template
     *            a template.
     * @return true if the specified template contains route parameters,
     *         otherwise false.
     */
    static boolean hasRequiredParameter(String template) {
        int index = -1;
        do {
            index = template.indexOf(':', index + 1);

            if (index >= 0) {
                final int regexIndex = template.indexOf('(', index);
                final int slashIndex = template.indexOf('/', index);

                int parameterNameEnding = Math.min(regexIndex, slashIndex);

                // Missing regex.
                if (parameterNameEnding < 0) {
                    parameterNameEnding = slashIndex;
                }
                // End of the string.
                if (parameterNameEnding < 0) {
                    parameterNameEnding = template.length();
                }

                int optional = template.indexOf('?', index);
                if (0 < optional && optional < parameterNameEnding) {
                    // This parameter is an optional, move on.
                    continue;
                }

                int wildcard = template.indexOf('*', index);
                if (0 < wildcard && wildcard < parameterNameEnding) {
                    // This parameter is a wildcard and should be the last.
                    return false;
                }

                // This parameter is required.
                return true;

            } else {
                // We reached the end of the search.
                return false;
            }

        } while (true);
    }

    static boolean isParameter(String segmentTemplate) {
        return segmentTemplate.startsWith(":");
    }

    static boolean isOptionalParameter(String segmentTemplate) {
        return isParameter(segmentTemplate) && (segmentTemplate.endsWith("?")
                || segmentTemplate.contains("?("));
    }

    static boolean isVarargsParameter(String segmentTemplate) {
        return isParameter(segmentTemplate) && (segmentTemplate.endsWith("*")
                || segmentTemplate.contains("*("));
    }

    static String getModifier(String segmentTemplate) {
        if (isOptionalParameter(segmentTemplate)) {
            return "?";
        } else if (isVarargsParameter(segmentTemplate)) {
            return "*";
        }

        return "";
    }

    static String getRegexName(String regex) {
        if (INTEGER_REGEX.equals(regex)) {
            return "integer";
        } else if (LONG_REGEX.equals(regex)) {
            return "long";
        } else if (BOOLEAN_REGEX.equals(regex)) {
            return "boolean";
        } else {
            return "string";
        }
    }

    static String formatSegment(RouteSegment segment,
            Set<RouteParameterFormatOption> format) {

        if (!segment.isParameter()) {
            return segment.getName();
        }

        StringBuilder result = new StringBuilder();

        result.append(":");

        final boolean formatRegex = format
                .contains(RouteParameterFormatOption.REGEX)
                || format.contains(RouteParameterFormatOption.REGEX_NAME);
        boolean wrapRegex = false;

        if (format.contains(RouteParameterFormatOption.NAME)) {
            result.append(segment.getName());
            wrapRegex = true;
        }

        if (format.contains(RouteParameterFormatOption.MODIFIER)) {
            result.append(getModifier(segment.getTemplate()));
            wrapRegex = true;
        }

        final String regex = formatSegmentRegex(segment, format);
        if (!regex.isEmpty() && formatRegex) {
            if (wrapRegex) {
                result.append("(");
            }

            result.append(regex);

            if (wrapRegex) {
                result.append(")");
            }
        }

        return result.toString();
    }

    static String formatSegmentRegex(RouteSegment segment,
            Set<RouteParameterFormatOption> format) {
        final String regex = segment.getRegex();
        if (format.contains(RouteParameterFormatOption.REGEX_NAME)) {
            return getRegexName(regex);
        } else {
            return regex;
        }
    }

    /**
     * Define a route url parameter details.
     */
    static class ParameterInfo implements Serializable {

        private String name;

        private String template;

        private boolean optional;

        private boolean varargs;

        private String regex;

        ParameterInfo(String template) {
            this.template = template;

            if (!isParameter(template)) {
                throw new IllegalArgumentException(
                        "The given string is not a parameter template.");
            }

            optional = isOptionalParameter(template);
            if (optional) {
                template = template.replaceFirst("\\?", "");
            }
            varargs = isVarargsParameter(template);
            if (varargs) {
                template = template.replaceFirst("\\*", "");
            }

            // Remove :
            template = template.substring(1);

            // Extract the template defining the value of the parameter.
            final int regexStartIndex = template.indexOf('(');
            if (regexStartIndex != -1) {

                name = template.substring(0, regexStartIndex);

                regex = template.substring(regexStartIndex + 1,
                        template.length() - 1);
            } else {
                name = template;
                regex = STRING_REGEX;
            }
        }

        public String getName() {
            return name;
        }

        public String getTemplate() {
            return template;
        }

        public boolean isOptional() {
            return optional;
        }

        public boolean isVarargs() {
            return varargs;
        }

        public String getRegex() {
            return regex;
        }
    }

}
