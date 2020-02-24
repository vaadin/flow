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
 * Container which stores the url parameters extracted from a navigation url
 * received from the client.
 */
public class UrlParameters implements Serializable {

    private Map<String, String> params;

    /**
     * Creates an empty UrlParameters instance.
     */
    public UrlParameters() {
        params = Collections.emptyMap();
    }

    /**
     * Creates a url parameters container using the given map as argument.
     * 
     * @param params
     *            parameters mapping.
     */
    public UrlParameters(Map<String, String> params) {
        this.params = params != null ? Collections.unmodifiableMap(params)
                : Collections.emptyMap();
    }

    /**
     * Creates a UrlParameters container using the given keys and values.
     *
     * @param keysAndValues
     *            parameters mapping.
     */
    public UrlParameters(String... keysAndValues) {
        Map<String, String> paramsMap = new HashMap<>(keysAndValues.length / 2);

        for (int i = 0; i < keysAndValues.length; i += 2) {
            paramsMap.put(keysAndValues[i], keysAndValues[i + 1]);
        }

        this.params = Collections.unmodifiableMap(paramsMap);
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
     *         parameter.
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
     *         parameter.
     */
    public Optional<Integer> getInt(String parameterName) {
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
     */
    public Optional<Long> getLong(String parameterName) {
        final String value = getValue(parameterName);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(value));
    }

    /**
     * Gets the boolean representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return an {@link Optional} {@link Boolean} representation of the
     *         parameter.
     */
    public Optional<Boolean> getBool(String parameterName) {
        final String value = getValue(parameterName);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(Boolean.valueOf(value));
    }

    /**
     * Gets a list representing the wildcard value of a parameter, where each
     * element in the list is a path segment. In case the value is missing the
     * result is an empty {@link List}.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return a {@link List} representing the wildcard value of a parameter, or
     *         an empty {@link List} is the wildcard is missing.
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
        if (obj instanceof UrlParameters) {
            UrlParameters urlParameters = (UrlParameters) obj;
            return params.equals(urlParameters.params);
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
