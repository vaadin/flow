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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.router.internal.PathUtil;

/**
 * Immutable container which stores the route parameters extracted from a
 * navigation url received from the client.
 */
public final class RouteParameters implements Serializable {

    private static final RouteParameters EMPTY = new RouteParameters();

    private Map<String, String> params;

    private RouteParameters() {
        params = Collections.emptyMap();
    }

    /**
     * Creates a RouteParameters container using the given map as argument.
     * 
     * @param params
     *            parameters mapping containing the parameter names mapping
     *            their values.
     */
    public RouteParameters(Map<String, String> params) {
        this.params = params != null ? Collections.unmodifiableMap(params)
                : Collections.emptyMap();
    }

    /**
     * Creates a RouteParameters container using the given {@link RouteParam}s.
     *
     * @param params
     *            the list of parameters and their values.
     * @throws IllegalArgumentException
     *             if a parameter is given more than once.
     */
    public RouteParameters(RouteParam... params) {
        Map<String, String> paramsMap = new HashMap<>(params.length);

        for (RouteParam param : params) {
            final String existingValue = paramsMap.put(param.getName(),
                    param.getValue());

            if (existingValue != null) {
                throw new IllegalArgumentException("Parameter "
                        + param.getName() + " is given more than once.");
            }
        }

        this.params = Collections.unmodifiableMap(paramsMap);
    }

    /**
     * Creates a RouteParameters instance using only one parameter.
     * 
     * @param name
     *            the name of the parameter.
     * @param value
     *            the value of the parameter.
     */
    public RouteParameters(String name, String value) {
        this.params = Collections.singletonMap(name, value);
    }

    /**
     * Creates an empty RouteParameters instance.
     * 
     * @return an empty instance of RouteParameters.
     */
    public static RouteParameters empty() {
        return EMPTY;
    }

    /**
     * Gets the available parameter names.
     * 
     * @return the available parameter names.
     */
    public Set<String> getParameterNames() {
        return params.keySet();
    }

    /**
     * Gets the string representation of a parameter.
     * 
     * @param parameterName
     *            the name of the parameter.
     * @return an {@link Optional} {@link String} representation of the
     *         parameter. If the value is missing the {@link Optional} is empty.
     */
    public Optional<String> get(String parameterName) {
        return Optional.ofNullable(getValue(parameterName));
    }

    /**
     * Gets the int representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return an {@link Optional} {@link Integer} representation of the
     *         parameter. If the value is missing the {@link Optional} is empty.
     * @exception NumberFormatException
     *                if the value cannot be parsed as an Integer.
     */
    public Optional<Integer> getInteger(String parameterName) {
        final String value = getValue(parameterName);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(Integer.valueOf(value));
    }

    /**
     * Gets the long representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return an {@link Optional} {@link Long} representation of the parameter.
     *         If the value is missing the {@link Optional} is empty.
     * @exception NumberFormatException
     *                if the value cannot be parsed as a Long.
     */
    public Optional<Long> getLong(String parameterName) {
        final String value = getValue(parameterName);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(value));
    }

    /**
     * Gets a list representing the wildcard value of a parameter, where each
     * element in the list is a path segment. In case the value is missing the
     * result is an empty {@link List}.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return a {@link List} representing the wildcard value of a parameter, or
     *         an empty {@link List} is the value is missing.
     */
    public List<String> getWildcard(String parameterName) {
        final String value = getValue(parameterName);
        if (value == null) {
            return Collections.emptyList();
        }

        return PathUtil.getSegmentsList(value);
    }

    @Override
    public String toString() {
        return params.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouteParameters) {
            RouteParameters parameters = (RouteParameters) obj;
            return params.equals(parameters.params);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return params.hashCode();
    }

    private String getValue(String parameterName) {
        return params.get(parameterName);
    }

}
