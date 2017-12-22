/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.internal.RouterUtil;

/**
 * Data collection of information for a specific registered route target.
 */
public class RouteData {
    private final Class<? extends RouterLayout> parentLayout;
    private final String url;
    private final List<Class<?>> parameters;
    private final Class<? extends Component> navigationTarget;
    private final List<AliasData> routeAliases;

    /**
     * Data class with information pertaining to the {@link RouteAlias}.
     */
    public static class AliasData {
        private final Class<? extends RouterLayout> parentLayout;
        private final String url;

        /**
         * Data class constructor.
         * 
         * @param parentLayout
         *            parent layout for alias
         * @param url
         *            target url for alias
         */
        public AliasData(Class<? extends RouterLayout> parentLayout,
                String url) {
            this.parentLayout = parentLayout;
            this.url = url;
        }

        /**
         * Getter for the {@link RouteAlias} parent layout.
         * 
         * @return parent layout for alias
         */
        public Class<? extends RouterLayout> getParentLayout() {
            return parentLayout;
        }

        /**
         * Getter for the {@link RouteAlias} url.
         * 
         * @return url of the alias
         */
        public String getUrl() {
            return url;
        }
    }

    /**
     * RouteData constructor.
     * 
     * @param parentLayout
     *            route parent layout class
     * @param url
     *            full route url
     * @param parameters
     *            navigation target path parameters
     * @param navigationTarget
     *            route navigation target
     */
    public RouteData(Class<? extends RouterLayout> parentLayout, String url,
            List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        this.parentLayout = parentLayout;
        this.url = url;
        this.parameters = parameters;
        this.navigationTarget = navigationTarget;
        this.routeAliases = new ArrayList<>();

        AnnotationReader.getAnnotationsFor(navigationTarget, RouteAlias.class)
                .forEach(alias -> routeAliases
                        .add(new AliasData(alias.layout(), RouterUtil
                                .getRouteAliasPath(navigationTarget, alias))));
        Collections.sort(routeAliases,
                (o1, o2) -> o1.getUrl().compareToIgnoreCase(o2.getUrl()));
    }

    /**
     * Get the parent layout of {@link Route}.
     * 
     * @return route parent layout
     */
    public Class<? extends RouterLayout> getParentLayout() {
        return parentLayout;
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
     * Get all {@link RouteAlias}es for this registered {@link Route}.
     * 
     * @return list of route aliases
     */
    public List<AliasData> getRouteAliases() {
        return Collections.unmodifiableList(routeAliases);
    }
}
