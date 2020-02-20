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
     * Gets the parameters as a {@link Map}.
     * 
     * @return a Map containing the parameter names and values.
     */
    public Map<String, String> getParameters() {
        return params;
    }

    /**
     * Gets the string representation of a parameter.
     * 
     * @param parameterName
     *            the name of the parameter.
     * @return the string representation of the parameter.
     */
    public String get(String parameterName) {
        return getValue(parameterName);
    }

    /**
     * Gets the int representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return the int representation of the parameter.
     */
    public Integer getInt(String parameterName) {
        final String value = get(parameterName);
        if (value == null) {
            return null;
        }

        return Integer.valueOf(value);
    }

    /**
     * Gets the long representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return the long representation of the parameter.
     */
    public Long getLong(String parameterName) {
        final String value = get(parameterName);
        if (value == null) {
            return null;
        }

        return Long.valueOf(value);
    }

    /**
     * Gets the boolean representation of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return the boolean representation of the parameter.
     */
    public Boolean getBool(String parameterName) {
        final String value = get(parameterName);
        if (value == null) {
            return null;
        }

        return Boolean.valueOf(value);
    }

    /**
     * Gets a list representing the wildcard value of a parameter, where each
     * element in the list is a path segment.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return a list representing the wildcard value of a parameter.
     */
    public List<String> getSegments(String parameterName) {
        final String value = get(parameterName);
        if (value == null) {
            return null;
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
