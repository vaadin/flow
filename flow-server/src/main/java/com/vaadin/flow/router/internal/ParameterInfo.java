/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Optional;

/**
 * Define a route url parameter details.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
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
