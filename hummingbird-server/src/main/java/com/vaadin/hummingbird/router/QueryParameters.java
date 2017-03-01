/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds query parameters information.
 *
 * @author Vaadin Ltd.
 */
public class QueryParameters implements Serializable {
    private final Map<String, List<String>> parameters;

    private QueryParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
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
        return new QueryParameters(
                Collections.unmodifiableMap(convertArraysToLists(parameters)));
    }

    private static Map<String, List<String>> convertArraysToLists(
            Map<String, String[]> fullParameters) {
        return fullParameters.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> Collections
                        .unmodifiableList(Arrays.asList(entry.getValue()))));
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
        return new QueryParameters(
                Collections.unmodifiableMap(toFullParameters(parameters)));
    }

    private static Map<String, List<String>> toFullParameters(
            Map<String, String> simpleParameters) {
        return simpleParameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Collections.singletonList(entry.getValue())));
    }

    /**
     * Returns query parameters information with support for multiple values
     * corresponding single name.
     * <p>
     * Example: {@code https://example.com/?one=1&two=2&one=3} will result in
     * the corresponding map: {@code {"one" : [1, 3], "two": [2]}}
     *
     * @return query parameters information
     */
    public Map<String, List<String>> getParameters() {
        return parameters;
    }
}
