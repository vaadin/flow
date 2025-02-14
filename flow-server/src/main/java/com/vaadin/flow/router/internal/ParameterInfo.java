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

package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Optional;

/**
 * Define a route url parameter details.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ParameterInfo implements Serializable {

    private final String name;

    private final String template;

    private final boolean optional;

    private final boolean varargs;

    private final String regex;

    public ParameterInfo(String template) {
        this.template = template;

        if (!RouteFormat.isParameter(template)) {
            throw new IllegalArgumentException(
                    "The given string is not a parameter template.");
        }

        optional = RouteFormat.isOptionalParameter(template);
        if (optional) {
            template = template.replaceFirst("\\?", "");
        }
        varargs = RouteFormat.isVarargsParameter(template);
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
            regex = null;
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

    public Optional<String> getRegex() {
        return Optional.ofNullable(regex);
    }
}