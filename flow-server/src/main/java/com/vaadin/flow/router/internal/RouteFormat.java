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

/**
 * Utility class which contains various methods for defining url parameter
 * template.
 */
class RouteFormat implements Serializable {

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
                        "Please provide a parameter template.");
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
                regex = "string";
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

    /**
     * Returns whether the specified pathTemplate contains url parameters.
     *
     * @param pathTemplate
     *            a path template.
     * @return true if the specified pathTemplate contains url parameters,
     *         otherwise false.
     */
    static boolean hasParameters(String pathTemplate) {
        return pathTemplate.contains(":");
    }

    static boolean isParameter(String segmentTemplate) {
        return segmentTemplate.contains(":");
    }

    static boolean isOptionalParameter(String segmentTemplate) {
        return isParameter(segmentTemplate) && (segmentTemplate.endsWith("?")
                || segmentTemplate.contains("?("));
    }

    static boolean isVarargsParameter(String segmentTemplate) {
        return isParameter(segmentTemplate) && (segmentTemplate.endsWith("*")
                || segmentTemplate.contains("*("));
    }

}
