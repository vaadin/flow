/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.router.internal.ParameterInfo;

/**
 * Immutable data representing one url parameter.
 *
 * @since 4.0
 */
public class RouteParameterData implements Serializable {

    private final String template;

    private final String regex;

    /**
     * Creates a parameter data instance.
     *
     * @param template
     *            the parameter template.
     * @param regex
     *            the regex as found in the template.
     */
    public RouteParameterData(String template, String regex) {
        this.template = template;
        this.regex = regex;
    }

    /**
     * Gets the parameter template string.
     *
     * @return the parameter template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Gets the regex of the parameter.
     *
     * @return the regex of the parameter.
     */
    public Optional<String> getRegex() {
        return Optional.ofNullable(regex);
    }

    /**
     * Return true for optional parameter.
     *
     * @return true for optional parameter
     * @since 24.5
     */
    public boolean isOptional() {
        return new ParameterInfo(getTemplate()).isOptional();
    }

    /**
     * Return true for parameter with varargs.
     *
     * @return true for parameter with varargs
     * @since 24.5
     */
    public boolean isVarargs() {
        return new ParameterInfo(getTemplate()).isVarargs();
    }
}
