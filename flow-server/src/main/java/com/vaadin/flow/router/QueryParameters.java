/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds query parameters information.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class QueryParameters implements Serializable {
    private static final String PARAMETER_VALUES_SEPARATOR = "=";
    private static final String PARAMETERS_SEPARATOR = "&";

    private final Map<String, List<String>> parameters;

    /**
     * Creates query parameters from parameter map.
     *
     * @param parameters
     *            the parameter map
     */
    public QueryParameters(Map<String, List<String>> parameters) {
        this.parameters = Collections
                .unmodifiableMap(parameters.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> Collections.unmodifiableList(
                                        new ArrayList<>(entry.getValue())))));
    }

    /**
     * Creates an empty query parameters information.
     *
     * @return query parameters information
     */
    public static QueryParameters empty() {
        return new QueryParameters(Collections.emptyMap());
    }

    /**
     * Creates parameters from full representation, where each parameter name
     * may correspond to multiple values.
     *
     * @param parameters
     *            query parameters map
     * @return query parameters information
     */
    public static QueryParameters full(Map<String, String[]> parameters) {
        return new QueryParameters(convertArraysToLists(parameters));
    }

    private static Map<String, List<String>> convertArraysToLists(
            Map<String, String[]> fullParameters) {
        return fullParameters.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, entry -> Arrays.asList(entry.getValue())));
    }

    /**
     * Creates parameters from simple representation, where each parameter name
     * corresponds to a single value.
     *
     * @param parameters
     *            query parameters map
     * @return query parameters information
     */
    public static QueryParameters simple(Map<String, String> parameters) {
        return new QueryParameters(toFullParameters(parameters));
    }

    private static Map<String, List<String>> toFullParameters(
            Map<String, String> simpleParameters) {
        return simpleParameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Collections.singletonList(entry.getValue())));
    }

    /**
     * Returns query parameters information with support for multiple values
     * corresponding to single parameter name.
     * <p>
     * Example: {@code https://example.com/?one=1&two=2&one=3} will result in
     * the corresponding map: {@code {"one" : [1, 3], "two": [2]}}
     *
     * @return query parameters information
     */
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    /**
     * Turns query parameters into query string that contains all parameter
     * names and their values. No guarantee on parameters' appearance order is
     * made.
     *
     * @return query string
     */
    public String getQueryString() {
        return parameters.entrySet().stream()
                .flatMap(this::getParameterAndValues)
                .collect(Collectors.joining(PARAMETERS_SEPARATOR));
    }

    private Stream<String> getParameterAndValues(
            Entry<String, List<String>> entry) {
        if (entry.getValue().isEmpty()) {
            return Stream.of(entry.getKey());
        }
        String param = entry.getKey();
        return entry.getValue().stream().map(value -> value == null ? param
                : param + PARAMETER_VALUES_SEPARATOR + value);
    }
}
