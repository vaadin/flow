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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.WildcardParameter;

/**
 * Utility methods to transform urls and parameters from/into the
 * {@link HasUrlParameter} format into/from the url template format.
 */
public class HasUrlParameterFormat implements Serializable {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}.
     */
    public static final String PARAMETER_NAME = "___url_parameter";

    private HasUrlParameterFormat() {
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

            if (!RouteFormat.STRING_REGEX.equals(type)) {
                urlBase += "(" + type + ")";
            }
        }
        return urlBase;
    }

    /**
     * Gets the url base without the parameter for the given url template and
     * navigation target implementing * {@link HasUrlParameter}.
     * 
     * @param urlTemplate
     *            the url template.
     * @return the url base excluding the parameter placeholder.
     */
    public static String getUrlBase(String urlTemplate) {
        if (RouteFormat.hasParameters(urlTemplate)) {
            return PathUtil.trimPath(
                    urlTemplate.substring(0, urlTemplate.indexOf(":")));
        }
        return urlTemplate;
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
     * Transform the {@link HasUrlParameter} value into a
     * {@link RouteParameters} object.
     *
     * @param parameter
     *            the parameter values.
     * @param <T>
     *            type of the input value.
     * @return RouteParameters instance wrapping the given parameter.
     */
    public static <T> RouteParameters getParameters(T parameter) {
        if (parameter == null) {
            return RouteParameters.empty();

        } else if (parameter instanceof String) {
            final List<String> segments = PathUtil
                    .getSegmentsList((String) parameter);
            if (segments.size() > 1) {
                return getParameters(segments);
            }
        }

        return new RouteParameters(
                Collections.singletonMap(PARAMETER_NAME, parameter.toString()));
    }

    /**
     * Transform the {@link HasUrlParameter} values into a
     * {@link RouteParameters} object.
     * 
     * @param parametersList
     *            the list of values.
     * @param <T>
     *            type of the input values.
     * @return RouteParameters instance wrapping the given parameters.
     */
    public static <T> RouteParameters getParameters(List<T> parametersList) {

        if (parametersList.size() == 1) {
            return getParameters(parametersList.get(0));
        }

        Map<String, String> map;

        if (parametersList.isEmpty()) {
            map = null;
        } else {
            map = Collections.singletonMap(PARAMETER_NAME,
                    parametersList.stream().map(T::toString)
                            .collect(Collectors.joining("/")));
        }

        return new RouteParameters(map);
    }

    /**
     * Gets the values for the {@link HasUrlParameter} from the specified url
     * parameters.
     * 
     * @param parameters
     *            url parameter.
     * @return HasUrlParameter compatible values.
     */
    public static List<String> getParameterValues(RouteParameters parameters) {

        List<String> wildcard = parameters
                .getWildcard(HasUrlParameterFormat.PARAMETER_NAME);

        if (wildcard.isEmpty()) {
            final Optional<String> value = parameters
                    .get(HasUrlParameterFormat.PARAMETER_NAME);

            if (value.isPresent()) {
                wildcard = Collections.singletonList(value.get());
            }
        }

        return wildcard;
    }

    /**
     * Gets the types of the parameters from string format.
     * 
     * @param types
     *            the input string format types.
     * @return the class types of the parameters.
     */
    public static List<Class<?>> getParameterTypes(Collection<String> types) {
        return types.stream().map(HasUrlParameterFormat::getType)
                .collect(Collectors.toList());
    }

    /**
     * Verify whether the navigationTarget has mandatory parameter and complies
     * with the given parameter values.
     * 
     * @param navigationTarget
     *            navigation target.
     * @param parameters
     *            navigation route parameters.
     */
    public static void checkMandatoryParameter(
            Class<? extends Component> navigationTarget,
            RouteParameters parameters) {
        if (hasUrlParameter(navigationTarget)
                && hasMandatoryParameter(navigationTarget)
                && (parameters == null
                        || !parameters.get(HasUrlParameterFormat.PARAMETER_NAME)
                                .isPresent())) {
            throw new IllegalArgumentException(String.format(
                    "Navigation target '%s' requires a parameter.",
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
            type = RouteFormat.INTEGER_REGEX;
        } else if (parameterType.isAssignableFrom(Long.class)) {
            type = RouteFormat.LONG_REGEX;
        } else if (parameterType.isAssignableFrom(Boolean.class)) {
            type = RouteFormat.BOOLEAN_REGEX;
        } else {
            type = RouteFormat.STRING_REGEX;
        }
        return type;
    }

    private static Class<?> getType(String regex) {
        if (RouteFormat.INTEGER_REGEX.equalsIgnoreCase(regex)) {
            return Integer.class;
        } else if (RouteFormat.LONG_REGEX.equalsIgnoreCase(regex)) {
            return Long.class;
        } else if (RouteFormat.BOOLEAN_REGEX.equalsIgnoreCase(regex)) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

}
