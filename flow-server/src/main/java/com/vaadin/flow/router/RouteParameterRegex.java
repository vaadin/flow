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

/**
 * Predefined regex used with template parameters.
 */
public class RouteParameterRegex implements Serializable {

    /**
     * Integer type regex.
     */
    public static final String INTEGER = "^[-+]?\\d+$";

    /**
     * Long type regex.
     */
    public static final String LONG = "^[+-]?[0-9]{1,19}$";

    /**
     * Boolean type regex.
     */
    public static final String BOOLEAN = "^true|false$";

    private RouteParameterRegex() {
    }

    /**
     * Gets the regex used for the given parameterType.
     *
     * @param parameterType
     *            type of the parameter.
     * @return the regex matching the type.
     */
    public static String getRegex(Class<?> parameterType) {
        String regex = null;
        if (parameterType.isAssignableFrom(Integer.class)) {
            regex = RouteParameterRegex.INTEGER;
        } else if (parameterType.isAssignableFrom(Long.class)) {
            regex = RouteParameterRegex.LONG;
        } else if (parameterType.isAssignableFrom(Boolean.class)) {
            regex = RouteParameterRegex.BOOLEAN;
        }
        return regex;
    }

    /**
     * Gets the type of the parameter for the given regex.
     *
     * @param regex
     *            the regex.
     * @return the type of the parameter.
     */
    public static Class<?> getType(String regex) {
        if (RouteParameterRegex.INTEGER.equalsIgnoreCase(regex)) {
            return Integer.class;
        } else if (RouteParameterRegex.LONG.equalsIgnoreCase(regex)) {
            return Long.class;
        } else if (RouteParameterRegex.BOOLEAN.equalsIgnoreCase(regex)) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    /**
     * Gets the name representation of the regex.
     *
     * @param regex
     *            the regex.
     * @return the name of the parameter type represented by the regex.
     */
    public static String getName(String regex) {
        return getType(regex).getSimpleName().toLowerCase();
    }

}
