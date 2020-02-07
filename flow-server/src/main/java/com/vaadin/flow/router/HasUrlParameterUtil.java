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
 *
 */
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;

public class HasUrlParameterUtil {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}
     */
    public static String PARAMETER_NAME = "___url_parameter";

    public static String getPathWithHasUrlParameter(String path,
            Class<? extends Component> navigationTarget) {
        if (hasUrlParameter(navigationTarget)) {

            if (hasOptionalParameter(navigationTarget)) {
                path += "/[:" + PARAMETER_NAME + "]";
            } else if (hasWildcardParameter(navigationTarget)) {
                path += "/...:" + PARAMETER_NAME;
            } else {
                path += "/:" + PARAMETER_NAME;
            }

            final Class<?> parameterType = ParameterDeserializer
                    .getClassType(navigationTarget);

            if (parameterType.isAssignableFrom(Integer.class)) {
                path += ":int";
            } else if (parameterType.isAssignableFrom(Long.class)) {
                path += ":long";
            } else if (parameterType.isAssignableFrom(Boolean.class)) {
                path += ":bool";
            }
        }
        return path;
    }

    public static <T> UrlParameters getParameters(T parameter) {
        return new UrlParameters(
                Collections.singletonMap(PARAMETER_NAME, parameter.toString()));
    }

    public static <T> UrlParameters getParameters(List<T> parametersList) {

        if (parametersList.size() == 1) {
            return getParameters(parametersList.get(0));
        }

        Map<String, Object> map;

        if (parametersList.isEmpty()) {
            map = null;
        } else {
            map = Collections.singletonMap(PARAMETER_NAME,
                    Collections.unmodifiableList(parametersList.stream()
                            .map(T::toString).collect(Collectors.toList())));
        }

        return new UrlParameters(map);
    }

    /**
     * Returns whether the target argument implements {@link HasUrlParameter}
     *
     * @param target
     *            target component class.
     * @return true if the target component class implements
     *         {@link HasUrlParameter}, otherwise false.
     */
    static boolean hasUrlParameter(Class<? extends Component> target) {
        return HasUrlParameter.class.isAssignableFrom(target);
    }

    /**
     * Returns whether the target class annotate the
     * {@link HasUrlParameter#setParameter(BeforeEvent, Object)} parameter with
     * {@link OptionalParameter}
     *
     * @param target
     *            target component class.
     * @return true if the target class annotate the
     *         {@link HasUrlParameter#setParameter(BeforeEvent, Object)}
     *         parameter with {@link OptionalParameter}, otherwise false.
     */
    static boolean hasOptionalParameter(Class<? extends Component> target) {
        return ParameterDeserializer.isAnnotatedParameter(target,
                OptionalParameter.class);
    }

    /**
     * Returns whether the target class annotate the
     * {@link HasUrlParameter#setParameter(BeforeEvent, Object)} parameter with
     * {@link WildcardParameter}
     *
     * @param target
     *            target component class.
     * @return true if the target class annotate the
     *         {@link HasUrlParameter#setParameter(BeforeEvent, Object)}
     *         parameter with {@link WildcardParameter}, otherwise false.
     */
    static boolean hasWildcardParameter(Class<? extends Component> target) {
        return ParameterDeserializer.isAnnotatedParameter(target,
                WildcardParameter.class);
    }

    private static boolean isAnnotatedParameter(
            Class<? extends Component> target) {
        return hasOptionalParameter(target) || hasWildcardParameter(target);
    }

    private HasUrlParameterUtil() {
    }
}
