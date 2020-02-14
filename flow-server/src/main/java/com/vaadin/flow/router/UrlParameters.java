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

/**
 * Container which stores the url parameters extracted from a navigation url
 * received from the client.
 */
public class UrlParameters implements Serializable {

    /**
     * Creates a url parameters container using the given keys and values.
     *
     * @param keysAndValues
     *            parameters mapping.
     */
    public static UrlParameters create(String... keysAndValues) {
        Map<String, Object> params = new HashMap<>(keysAndValues.length / 2);

        for (int i = 0; i < keysAndValues.length; i++) {
            params.put(keysAndValues[i], keysAndValues[++i]);
        }

        return new UrlParameters(params);
    }

    private Map<String, Object> params;

    /**
     * Creates a url parameters container using the given map as argument.
     * 
     * @param params
     *            parameters mapping.
     */
    public UrlParameters(Map<String, Object> params) {
        this.params = params != null ? Collections.unmodifiableMap(params)
                : Collections.emptyMap();
    }

    /**
     * Gets the available parameter names.
     * 
     * @return the available parameter names.
     */
    public Set<String> getAvailableParameterNames() {
        return params.keySet();
    }

    public Map<String, Object> getParameters() {
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
        final Object value = getObject(parameterName);
        if (value == null) {
            return null;
        }

        return value.toString();
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

        return new Integer(value);
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

        return new Long(value);
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

        return new Boolean(value);
    }

    /**
     * Gets a list representing the varargs value of a parameter.
     *
     * @param parameterName
     *            the name of the parameter.
     * @return a list representing the varargs value of a parameter.
     */
    public List<String> getList(String parameterName) {
        final Object value = getObject(parameterName);
        if (value == null) {
            return null;
        }

        if (value instanceof List) {
            // This should be already unmodifiable but from here we can't really
            // guarantee.
            return Collections.unmodifiableList((List<String>) value);
        } else {
            return null;
        }
    }

    /**
     * Gets the actual object value of the parameter. Currently this is always a
     * string.
     * 
     * @param parameterName
     *            the name of the parameter.
     * @return the actual object value of the parameter.
     */
    private Object getObject(String parameterName) {
        return params.get(parameterName);
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
}
