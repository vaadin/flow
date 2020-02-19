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

import com.vaadin.flow.component.Component;

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
    private final String urlTemplate;
    private final Map<String, String> parameters;
    private final Class<? extends Component> navigationTarget;

    /**
     * RouteBaseData constructor.
     *
     * @param parentLayouts
     *         route parent layout class chain
     * @param urlTemplate
     *         full route url
     * @param parameters
     *         navigation target path parameters
     * @param navigationTarget
     *         route navigation target
     * @deprecated use {@link #RouteBaseData(List, String, Map, Class)} instead.
     */
    @Deprecated
    public RouteBaseData(List<Class<? extends RouterLayout>> parentLayouts,
            String urlTemplate, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        this.parentLayouts = Collections.unmodifiableList(parentLayouts);
        this.urlTemplate = urlTemplate;
        this.parameters = Collections.emptyMap();
        this.navigationTarget = navigationTarget;
    }

    /**
     * RouteBaseData constructor.
     *
     * @param parentLayouts
     *         route parent layout class chain
     * @param urlTemplate
     *         full route url
     * @param parameters
     *         navigation target path parameters
     * @param navigationTarget
     *         route navigation target
     */
    public RouteBaseData(List<Class<? extends RouterLayout>> parentLayouts,
            String urlTemplate, Map<String, String> parameters,
            Class<? extends Component> navigationTarget) {
        this.parentLayouts = Collections.unmodifiableList(parentLayouts);
        this.urlTemplate = urlTemplate;
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
     * @deprecated use {@link #getUrlTemplate()} instead.
     */
    @Deprecated
    public String getUrl() {
        return urlTemplate;
    }

    /**
     * Get the full route url of {@link Route}.
     *
     * @return route url template.
     */
    public String getUrlTemplate() {
        return urlTemplate;
    }

    /**
     * Get {@link Route} url parameters if any.
     *
     * @return url parameters (by type and in order)
     * @deprecated use {@link #getDefinedParameters()} instead.
     */
    @Deprecated
    public List<Class<?>> getParameters() {
        return HasUrlParameterUtil.getParameterTypes(parameters.values());
    }

    /**
     * Get {@link Route} url parameters if any.
     *
     * @return url parameters names mapped with their types.
     */
    public Map<String, String> getDefinedParameters() {
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
        return this.getUrl().compareToIgnoreCase(otherRouteData.getUrl());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteBaseData<?>) {
            RouteBaseData<?> other = (RouteBaseData<?>) obj;
            return other.parentLayouts.equals(this.parentLayouts) && other.urlTemplate
                    .equals(this.urlTemplate) && other.navigationTarget
                    .equals(navigationTarget);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentLayouts, urlTemplate, navigationTarget);
    }
}
