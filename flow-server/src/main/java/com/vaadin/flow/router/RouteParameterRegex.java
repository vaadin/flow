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
    public static final String LONG = "^[+-]?[0-8]?[0-9]{1,18}$";

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
