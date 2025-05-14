/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.RouteParameterRegex;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.WildcardParameter;

/**
 * Utility methods to transform urls and parameters from/into the
 * {@link HasUrlParameter} format into/from the template format.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class HasUrlParameterFormat implements Serializable {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}.
     */
    public static final String PARAMETER_NAME = "___url_parameter";

    /**
     * Reserved parameter placeholder used when setup internally route path
     * pattern with the parameter design for backward compatibility with
     * {@link HasUrlParameter}.
     */
    public static final String PARAMETER = ":" + PARAMETER_NAME;

    private HasUrlParameterFormat() {
    }

    /**
     * Gets the template for the given url base by appending the parameter
     * according to the given navigationTarget if it's implementing
     * {@link HasUrlParameter}
     *
     * @param urlBase
     *            url base.
     * @param navigationTarget
     *            {@link HasUrlParameter} navigation target.
     * @return the final template.
     */
    public static String getTemplate(String urlBase,
            Class<? extends Component> navigationTarget) {
        if (hasUrlParameter(navigationTarget)) {

            urlBase = PathUtil.trimPath(urlBase);

            if (hasOptionalParameter(navigationTarget)) {
                urlBase += "/" + PARAMETER + "?";
            } else if (hasWildcardParameter(navigationTarget)) {
                urlBase += "/" + PARAMETER + "*";
            } else {
                urlBase += "/" + PARAMETER;
            }

            final Class<?> parameterType = ParameterDeserializer
                    .getClassType(navigationTarget);

            if (!String.class.equals(parameterType)) {
                urlBase += "(" + RouteParameterRegex.getRegex(parameterType)
                        + ")";
            }
        }
        return urlBase;
    }

    /**
     * Gets the url base without the parameter for the given template and
     * navigation target implementing * {@link HasUrlParameter}.
     *
     * @param template
     *            the template.
     * @return the url base excluding the parameter placeholder.
     */
    public static String getUrlBase(String template) {
        if (RouteFormat.hasParameters(template)) {
            return PathUtil
                    .trimPath(template.substring(0, template.indexOf(':')));
        }
        return template;
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

        RouteParameters result;

        if (parametersList.isEmpty()) {
            result = RouteParameters.empty();
        } else {
            result = new RouteParameters(Collections.singletonMap(
                    PARAMETER_NAME, parametersList.stream().map(T::toString)
                            .collect(Collectors.joining("/"))));
        }

        return result;
    }

    /**
     * Gets the values for the {@link HasUrlParameter} from the specified route
     * parameters.
     *
     * @param parameters
     *            route parameter.
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
        return types.stream().map(RouteParameterRegex::getType)
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
     * Returns whether the target argument implements {@link HasUrlParameter}.
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

}
