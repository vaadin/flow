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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;

/**
 * Data collection of information for a specific registered route target.
 */
public class RouteData implements Comparable<RouteData>, Serializable {
    private final List<Class<? extends RouterLayout>> parentLayouts;
    private final String url;
    private final List<Class<?>> parameters;
    private final Class<? extends Component> navigationTarget;
    private final List<AliasData> routeAliases;

    /**
     * Data class with information pertaining to the {@link RouteAlias}.
     */
    public static class AliasData
            implements Comparable<AliasData>, Serializable {
        private final List<Class<? extends RouterLayout>> parentLayouts;
        private final String url;

        /**
         * Data class constructor.
         *
         * @param parentLayout
         *         parent layout for alias
         * @param url
         *         target url for alias
         */
        public AliasData(List<Class<? extends RouterLayout>> parentLayout,
                String url) {
            this.parentLayouts = parentLayout;
            this.url = url;
        }

        /**
         * Getter for the {@link RouteAlias} parent layout.
         *
         * @return parent layout for alias
         */
        public Class<? extends RouterLayout> getParentLayout() {
            if (parentLayouts.isEmpty()) {
                return null;
            }
            return parentLayouts.get(0);
        }

        /**
         * Getter for the {@link RouteAlias} url.
         *
         * @return url of the alias
         */
        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(AliasData otherAlias) {
            return this.url.compareToIgnoreCase(otherAlias.url);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AliasData) {
                AliasData other = (AliasData) obj;
                return other.getParentLayout().equals(this.getParentLayout())
                        && other.url.equals(this.url);
            }
            return false;
        }
    }

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
            List<AliasData> routeAliases) {
        this.parentLayouts = parentLayouts;
        this.url = url;
        this.parameters = Collections.unmodifiableList(parameters);
        this.navigationTarget = navigationTarget;
        this.routeAliases = new ArrayList<>(routeAliases);

        Collections.sort(this.routeAliases);
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
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Get {@link Route} navigation target.
     *
     * @return navigation target
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Get all RouteAliases for this registered path.
     *
     * @return list of route aliases
     */
    public List<AliasData> getRouteAliases() {
        return Collections.unmodifiableList(routeAliases);
    }

    @Override
    public int compareTo(RouteData otherRouteData) {
        return this.getUrl().compareToIgnoreCase(otherRouteData.getUrl());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteData) {
            RouteData other = (RouteData) obj;
            return other.getParentLayout().equals(this.getParentLayout())
                    && other.url.equals(this.url) && other.navigationTarget
                    .equals(navigationTarget);
        }
        return false;
    }

    @Override
    public String toString() {
        return "RouteData{" + "parentLayout=" + getParentLayout() + ", url='"
                + url + '\'' + ", parameters=" + parameters
                + ", navigationTarget=" + navigationTarget + ", routeAliases="
                + routeAliases + '}';
    }
}
