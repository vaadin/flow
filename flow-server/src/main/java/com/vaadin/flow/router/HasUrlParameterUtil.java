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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.PathUtil;

/**
 * Utility methods to transform urls and parameters from/into the
 * {@link HasUrlParameter} format into/from the url template format.
 */
public class HasUrlParameterUtil {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}
     */
    public final static String PARAMETER_NAME = "___url_parameter";

    private HasUrlParameterUtil() {
    }

    /**
     * Gets the url template for the given url base by appending the parameter
     * according to the given navigationTarget if it's implementing
     * {@link HasUrlParameter}
     * 
     * @param urlBase
     *            url base.
     * @param navigationTarget
     *            {@link HasUrlParameter} navigation target.
     * @return the final url template.
     */
    public static String getUrlTemplate(String urlBase,
            Class<? extends Component> navigationTarget) {
        if (hasUrlParameter(navigationTarget)) {

            if (hasOptionalParameter(navigationTarget)) {
                urlBase += "/:" + PARAMETER_NAME + "?";
            } else if (hasWildcardParameter(navigationTarget)) {
                urlBase += "/:" + PARAMETER_NAME + "*";
            } else {
                urlBase += "/:" + PARAMETER_NAME;
            }

            final Class<?> parameterType = ParameterDeserializer
                    .getClassType(navigationTarget);

            String type = getParameterType(parameterType);

            if (!"string".equals(type)) {
                urlBase += "(" + type + ")";
            }
        }
        return urlBase;
    }

    /**
     * Gets the final url by appending the given parameters.
     * 
     * @param url
     *            url base.
     * @param parameters
     *            {@link HasUrlParameter} parameter values.
     * @param <T>
     *            type of the values.
     * @return navigation url string.
     */
    public static <T> String getUrl(String url, List<T> parameters) {
        return PathUtil.getPath(url, parameters.stream().map(T::toString)
                .collect(Collectors.toList()));
    }

    /**
     * Transform the {@link HasUrlParameter} value into a {@link UrlParameters}
     * object.
     *
     * @param parameter
     *            the parameter values.
     * @param <T>
     *            type of the input value.
     * @return UrlParameters instance wrapping the given parameter.
     */
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

    /**
     * Transform the {@link HasUrlParameter} values into a {@link UrlParameters}
     * object.
     * 
     * @param parametersList
     *            the list of values.
     * @param <T>
     *            type of the input values.
     * @return UrlParameters instance wrapping the given parameters.
     */
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
     * Gets the values for the {@link HasUrlParameter} from the specified url
     * parameters.
     * 
     * @param urlParameters
     *            url parameter.
     * @return HasUrlParameter compatible values.
     */
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

    /**
     * Gets the types of the parameters from string format.
     * 
     * @param types
     *            the input string format types.
     * @return the class types of the parameters.
     */
    public static List<Class<?>> getParameterTypes(Collection<String> types) {
        return types.stream().map(HasUrlParameterUtil::getType)
                .collect(Collectors.toList());
    }

    /**
     * Verify whether the navigationTarget has mandatory parameter and complies
     * with the given parameter values.
     * 
     * @param navigationTarget
     *            navigation target.
     * @param parameters
     *            navigation url parameters.
     */
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
        if ("int".equalsIgnoreCase(s)) {
            return Integer.class;
        } else if ("long".equalsIgnoreCase(s)) {
            return Long.class;
        } else if ("boolean".equalsIgnoreCase(s)
                || "bool".equalsIgnoreCase(s)) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

}
