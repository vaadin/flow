/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.Optional;

/**
 * Immutable data representing one url parameter.
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
}
