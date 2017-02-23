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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class that holds request parameters information.
 * <p>
 * For convenience reasons, the information can be accessed in two forms:
 * <ul>
 * <li>Simple, where each parameter name corresponds to a single value:
 * {@link RequestParameters#getSimpleParameterMap()}</li>
 * <li>Full, where each parameter name name may correspond to multiple values:
 * {@link RequestParameters#getFullParameterMap()}</li>
 * </ul>
 *
 * @author Vaadin Ltd.
 */
public class RequestParameters implements Serializable {
    private final Map<String, String[]> fullParameterMap;
    private final Map<String, String> simpleParameterMap;

    private RequestParameters(Map<String, String[]> fullParameterMap,
            Map<String, String> simpleParameterMap) {
        this.fullParameterMap = fullParameterMap;
        this.simpleParameterMap = simpleParameterMap;
    }

    /**
     * Creates an empty request parameters information.
     *
     * @return request parameters information
     */
    public static RequestParameters empty() {
        return new RequestParameters(Collections.emptyMap(),
                Collections.emptyMap());
    }

    /**
     * Creates parameters from full representation, where each parameter name
     * may correspond to multiple values.
     * <p>
     * For each key in full representation, simple representation will contain
     * first element of the array.
     *
     * @param parameters
     *            request parameters map
     * @return request parameters information
     */
    public static RequestParameters full(Map<String, String[]> parameters) {
        return new RequestParameters(parameters,
                toSimpleParameters(parameters));
    }

    /**
     * Creates parameters from simple representation, where each parameter name
     * corresponds to a single value.
     * <p>
     * For each key in simple representation, full representation will contain a
     * single-sized array with corresponding value.
     *
     * @param parameters
     *            request parameters map
     * @return request parameters information
     */
    public static RequestParameters simple(Map<String, String> parameters) {
        return new RequestParameters(toFullParameters(parameters), parameters);
    }

    /**
     * Returns full request parameters information, assuming that each parameter
     * name may correspond to multiple values.
     * <p>
     * Example: {@code https://example.com/?one=1&two=2&one=3} will result in
     * the corresponding map: {@code {"one" : [1, 3], "two": [2]}}
     *
     * @return full request parameters information
     */
    public Map<String, String[]> getFullParameterMap() {
        return fullParameterMap;
    }

    /**
     * Returns simple request parameters information, where each parameter name
     * corresponds to a single value.
     * <p>
     * Example: {@code https://example.com/?one=1&two=2&one=3} will result in
     * the corresponding map: {@code {"one" : 1, "two": 2}}
     *
     * @return full request parameters information
     */
    public Map<String, String> getSimpleParameterMap() {
        return simpleParameterMap;
    }

    private static Map<String, String[]> toFullParameters(
            Map<String, String> simpleParameters) {
        return simpleParameters.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, entry -> new String[] { entry.getValue() }));
    }

    private static Map<String, String> toSimpleParameters(
            Map<String, String[]> fullParameters) {
        return fullParameters.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(entry -> entry.getValue().length > 0)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue()[0]));
    }
}
