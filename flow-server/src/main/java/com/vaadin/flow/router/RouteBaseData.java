/*
 * Copyright 2000-2018 Vaadin Ltd.
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
    private final String url;
    private final List<Class<?>> parameters;
    private final Class<? extends Component> navigationTarget;

    /**
     * RouteBaseData constructor.
     *
     * @param parentLayouts
     *         route parent layout class chain
     * @param url
     *         full route url
     * @param parameters
     *         navigation target path parameters
     * @param navigationTarget
     *         route navigation target
     */
    public RouteBaseData(List<Class<? extends RouterLayout>> parentLayouts,
            String url, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        this.parentLayouts = Collections.unmodifiableList(parentLayouts);
        this.url = url;
        this.parameters = Collections.unmodifiableList(parameters);
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
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get {@link Route} url parameters if any.
     *
     * @return url parameters (by type and in order)
     */
    public List<Class<?>> getParameters() {
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
            return other.parentLayouts.equals(this.parentLayouts) && other.url
                    .equals(this.url) && other.navigationTarget
                    .equals(navigationTarget);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentLayouts, url, navigationTarget);
    }
}
