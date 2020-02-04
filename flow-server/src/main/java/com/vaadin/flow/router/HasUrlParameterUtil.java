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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.WildcardParameter;

public class HasUrlParameterUtil {

    /**
     * Reserved parameter name used when setup internally route path pattern
     * with the parameter design for backward compatibility with
     * {@link HasUrlParameter}
     */
    public static String PARAMETER_NAME = "___url_parameter";

    public static String getPathWithHasUrlParameter(String pathPattern,
            Class<? extends Component> navigationTarget) {
        if (hasUrlParameter(navigationTarget)) {

            if (hasOptionalParameter(navigationTarget)) {
                pathPattern += "/[:" + PARAMETER_NAME + "]";
            } else if (hasWildcardParameter(navigationTarget)) {
                pathPattern += "/...:" + PARAMETER_NAME;
            } else {
                pathPattern += "/:" + PARAMETER_NAME;
            }

            final Class<?> parameterType = ParameterDeserializer
                    .getClassType(navigationTarget);

            if (parameterType.isAssignableFrom(Integer.class)) {
                pathPattern += ":int";
            } else if (parameterType.isAssignableFrom(Long.class)) {
                pathPattern += ":long";
            } else if (parameterType.isAssignableFrom(Boolean.class)) {
                pathPattern += ":bool";
            }
        }
        return pathPattern;
    }

    public static <T> UrlParameters getParameters(List<T> parametersList) {
        // TODO: handle wildcard
        Map<String, Serializable> map = Collections.singletonMap(PARAMETER_NAME,
                parametersList.get(0).toString());

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
