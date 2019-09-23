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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;

/**
 * Data collection of information for a specific registered route target.
 * <p>
 * Only for read as data is immutable.
 *
 * @since 1.0
 */
public class RouteData extends RouteBaseData<RouteData> {
    private final List<RouteAliasData> routeAliases;

    /**
     * RouteData constructor.
     *
     * @param parentLayouts
     *         route parent layout class chain
     * @param url
     *         full route url
     * @param parameters
     *         navigation target path parameters
     * @param navigationTarget
     *         route navigation target
     * @param routeAliases
     *         list of aliases for this route
     */
    public RouteData(List<Class<? extends RouterLayout>> parentLayouts,
            String url, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget,
            List<RouteAliasData> routeAliases) {
        super(parentLayouts, url, parameters, navigationTarget);

        Collections.sort(routeAliases);
        this.routeAliases = Collections.unmodifiableList(routeAliases);
    }

    /**
     * Get all RouteAliases for this registered path.
     *
     * @return list of route aliases
     */
    public List<RouteAliasData> getRouteAliases() {
        return routeAliases;
    }

    @Override
    public String toString() {
        return "RouteData{" + "parentLayout=" + getParentLayout() + ", url='"
                + getUrl() + '\'' + ", parameters=" + getParameters()
                + ", navigationTarget=" + getNavigationTarget()
                + ", routeAliases=" + routeAliases + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteData) {
            RouteData other = (RouteData) obj;
            return other.getParentLayouts().equals(this.getParentLayouts())
                    && other.getUrl().equals(this.getUrl()) && other
                    .getNavigationTarget().equals(getNavigationTarget())
                    && routeAliases.containsAll(other.routeAliases);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParentLayouts(), getUrl(), getNavigationTarget(),
                routeAliases);
    }
}
