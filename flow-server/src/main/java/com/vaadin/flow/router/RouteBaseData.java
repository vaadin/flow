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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;

/**
 * Abstract base class for route and route alias data.
 * <p>
 * Only for read as data is immutable.
 *
 * @param <T>
 *         base data implementation
 * @since 1.3
 */
public abstract class RouteBaseData<T extends RouteBaseData>
        implements Comparable<T>, Serializable {

    private final List<Class<? extends RouterLayout>> parentLayouts;
    private final String template;
    private final Map<String, RouteParameterData> parameters;
    private final Class<? extends Component> navigationTarget;

    /**
     * RouteBaseData constructor. This constructor doesn't support parameters.
     * When a non empty List is provided {@link IllegalArgumentException} is
     * raised.
     *
     * @param parentLayouts
     *            route parent layout class chain
     * @param template
     *            full route url
     * @param parameters
     *            supports only null or empty list. If a non empty list is
     *            passed and {@link IllegalArgumentException} is raised.
     * @param navigationTarget
     *            route navigation target
     * @throws IllegalArgumentException
     *             if parameters is not empty.
     */
    public RouteBaseData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        if (!(parameters == null || parameters.isEmpty())) {
            throw new IllegalArgumentException(
                    "Please provide an empty parameters list or use the constructor receiving the parameters as a Map");
        }

        this.parentLayouts = Collections.unmodifiableList(parentLayouts);
        this.template = template;
        this.parameters = Collections.emptyMap();
        this.navigationTarget = navigationTarget;
    }

    /**
     * RouteBaseData constructor.
     *
     * @param parentLayouts
     *         route parent layout class chain
     * @param template
     *         full route url
     * @param parameters
     *         navigation target path parameters
     * @param navigationTarget
     *         route navigation target
     */
    public RouteBaseData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, Map<String, RouteParameterData> parameters,
            Class<? extends Component> navigationTarget) {
        this.parentLayouts = Collections.unmodifiableList(parentLayouts);
        this.template = template;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.navigationTarget = navigationTarget;
    }

    /**
     * Get the parent layout of {@link Route}.
     *
     * @return route parent layout
     */
    public Class<? extends RouterLayout> getParentLayout() {
        if (parentLayouts.isEmpty()) {
            return null;
        }
        return parentLayouts.get(0);
    }

    /**
     * Return the whole parent layout chain of route target.
     *
     * @return route parent layout chain
     */
    public List<Class<? extends RouterLayout>> getParentLayouts() {
        return parentLayouts;
    }

    /**
     * Get the full route url of {@link Route}.
     *
     * @return route url
     * @deprecated use {@link #getTemplate()} instead.
     */
    @Deprecated
    public String getUrl() {
        return template;
    }

    /**
     * Get the full route template of {@link Route}.
     *
     * @return route template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Get {@link Route} route parameters types if any.
     *
     * @return route parameters types.
     * @deprecated use {@link #getRouteParameters()} instead.
     */
    @Deprecated
    public List<Class<?>> getParameters() {
        final List<String> parametersRegex = parameters.values().stream()
                .map(RouteParameterData::getRegex).map(Optional::get)
                .collect(Collectors.toList());
        return HasUrlParameterFormat.getParameterTypes(parametersRegex);
    }

    /**
     * Get {@link Route} route parameters if any.
     *
     * @return route parameters names mapped with their defined regex.
     */
    public Map<String, RouteParameterData> getRouteParameters() {
        return parameters;
    }

    /**
     * Get {@link Route} navigation target.
     *
     * @return navigation target
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    @Override
    public int compareTo(T otherRouteData) {
        return this.getTemplate()
                .compareToIgnoreCase(otherRouteData.getTemplate());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteBaseData<?>) {
            RouteBaseData<?> other = (RouteBaseData<?>) obj;
            return other.parentLayouts.equals(this.parentLayouts)
                    && other.template.equals(this.template)
                    && other.navigationTarget.equals(navigationTarget);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentLayouts, template, parameters,
                navigationTarget);
    }
}
