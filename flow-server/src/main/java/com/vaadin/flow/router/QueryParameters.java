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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.UrlUtil;

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
     * Creates parameters from a query string.
     * <p>
     * Note that no length checking is done for the string. It is the
     * responsibility of the caller (or the server) to limit the length of the
     * query string.
     *
     * @param queryString
     *            the query string
     * @return query parameters information
     */
    public static QueryParameters fromString(String queryString) {
        return new QueryParameters(parseQueryString(queryString));
    }

    private static Map<String, List<String>> parseQueryString(String query) {
        Map<String, List<String>> parsedParams = Arrays
                .stream(query.split(PARAMETERS_SEPARATOR))
                .map(QueryParameters::makeQueryParamList)
                .collect(Collectors.toMap(list -> list.get(0),
                        QueryParameters::getParameterValues,
                        QueryParameters::mergeLists));
        return parsedParams;
    }

    private static List<String> makeQueryParamList(String paramAndValue) {
        int index = paramAndValue.indexOf('=');
        if (index == -1) {
            return Collections.singletonList(decode(paramAndValue));
        }
        String param = paramAndValue.substring(0, index);
        String value = paramAndValue.substring(index + 1);
        return Arrays.asList(decode(param), decode(value));
    }

    private static List<String> getParameterValues(List<String> paramAndValue) {
        if (paramAndValue.size() == 1) {
            return Collections.singletonList("");
        } else {
            return Collections.singletonList(paramAndValue.get(1));
        }
    }

    private static List<String> mergeLists(List<String> list1,
            List<String> list2) {
        List<String> result = new ArrayList<>(list1);
        if (result.isEmpty()) {
            result.add(null);
        }
        if (list2.isEmpty()) {
            result.add(null);
        } else {
            result.addAll(list2);
        }

        return result;
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
     * Returns a UTF-8 encoded query string containing all parameter names and
     * values suitable for appending to a URL after the {@code ?} character.
     * Parameters may appear in different order than in the query string they
     * were originally parsed from, and may be differently encoded (for example,
     * if a space was encoded as {@code +} in the initial URL it will be encoded
     * as {@code %20} in the result.
     *
     * @return query string suitable for appending to a URL
     * @see URLEncoder#encode(String, String)
     */
    public String getQueryString() {
        return parameters.entrySet().stream()
                .flatMap(this::getParameterAndValues)
                .collect(Collectors.joining(PARAMETERS_SEPARATOR));
    }

    private Stream<String> getParameterAndValues(
            Entry<String, List<String>> entry) {
        String param = entry.getKey();
        List<String> values = entry.getValue();
        if (values.size() == 1 && "".equals(values.get(0))) {
            return Stream.of(UrlUtil.encodeURIComponent(entry.getKey()));
        }
        return values.stream()
                .map(value -> "".equals(value)
                        ? UrlUtil.encodeURIComponent(param)
                        : UrlUtil.encodeURIComponent(param)
                                + PARAMETER_VALUES_SEPARATOR
                                + UrlUtil.encodeURIComponent(value));
    }

    private static String decode(String parameter) {
        try {
            return URLDecoder.decode(parameter, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "Unable to decode parameter: " + parameter, e);
        }
    }
}
