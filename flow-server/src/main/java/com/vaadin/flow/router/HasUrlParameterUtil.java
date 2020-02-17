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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.PathUtil;

public class HasUrlParameterUtil {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}
     */
    public static String PARAMETER_NAME = "___url_parameter";

    public static String getPathTemplate(String path,
            Class<? extends Component> navigationTarget) {
        if (hasUrlParameter(navigationTarget)) {

            if (hasOptionalParameter(navigationTarget)) {
                path += "/:" + PARAMETER_NAME + "?";
            } else if (hasWildcardParameter(navigationTarget)) {
                path += "/:" + PARAMETER_NAME + "*";
            } else {
                path += "/:" + PARAMETER_NAME;
            }

            final Class<?> parameterType = ParameterDeserializer
                    .getClassType(navigationTarget);

            String type = getParameterType(parameterType);

            if (!"string".equals(type)) {
                path += "(" + type + ")";
            }
        }
        return path;
    }

    public static <T> UrlParameters getParameters(T parameter) {
        if (parameter == null) {
            return new UrlParameters(null);

        } else if (parameter instanceof String) {
            final List<String> segments = PathUtil
                    .getSegmentsList((String) parameter);
            if (segments.size() > 1) {
                return getParameters(segments);
            }
        }

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

    public static List<String> getParameterValues(UrlParameters urlParameters) {

        List<String> parameters = urlParameters
                .getList(HasUrlParameterUtil.PARAMETER_NAME);

        if (parameters == null) {
            final String value = urlParameters
                    .get(HasUrlParameterUtil.PARAMETER_NAME);

            if (value != null) {
                parameters = Collections.singletonList(value);
            }
        }

        if (parameters == null) {
            parameters = Collections.emptyList();
        }

        return parameters;
    }

    public static List<Class<?>> getParameterTypes(Collection<String> types) {
        return types.stream().map(HasUrlParameterUtil::getType)
                .collect(Collectors.toList());
    }

    public static void checkMandatoryParameter(
            Class<? extends Component> navigationTarget,
            UrlParameters parameters) {
        if (hasUrlParameter(navigationTarget)
                && hasMandatoryParameter(navigationTarget)
                && (parameters == null || parameters
                        .get(HasUrlParameterUtil.PARAMETER_NAME) == null)) {
            throw new IllegalArgumentException(String.format(
                    "Navigation target '%s' requires a parameter and can not be resolved. "
                            + "Use 'public <T, C extends Component & HasUrlParameter<T>> "
                            + "String getUrl(Class<? extends C> navigationTarget, T parameter)' "
                            + "instead",
                    navigationTarget.getName()));
        }
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
     * Returns whether the target class doesn't annotate the
     * {@link HasUrlParameter#setParameter(BeforeEvent, Object)} with neither
     * {@link OptionalParameter} nor {@link WildcardParameter}
     *
     * @param target
     *            target component class.
     * @return true if the target class doesn't annotate the
     *         {@link HasUrlParameter#setParameter(BeforeEvent, Object)} with
     *         neither {@link OptionalParameter} nor {@link WildcardParameter},
     *         otherwise false.
     */
    static boolean hasMandatoryParameter(Class<? extends Component> target) {
        return !(hasOptionalParameter(target) || hasWildcardParameter(target));
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

    private static String getParameterType(Class<?> parameterType) {
        String type = null;
        if (parameterType.isAssignableFrom(Integer.class)) {
            type = "int";
        } else if (parameterType.isAssignableFrom(Long.class)) {
            type = "long";
        } else if (parameterType.isAssignableFrom(Boolean.class)) {
            type = "boolean";
        } else {
            type = "string";
        }
        return type;
    }

    private static Class<?> getType(String s) {
        if (s.equalsIgnoreCase("int")) {
            return Integer.class;
        } else if (s.equalsIgnoreCase("long")) {
            return Long.class;
        } else if (s.equalsIgnoreCase("boolean")
                || s.equalsIgnoreCase("bool")) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    private HasUrlParameterUtil() {
    }

}
