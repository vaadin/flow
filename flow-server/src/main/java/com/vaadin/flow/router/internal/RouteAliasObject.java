/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;

/**
 * A data transfer object to keep information the same as {@link RouteAlias}
 * contains using plain Java object.
 * 
 * @author Vaadin Ltd
 * @since
 * 
 * @see RouteAlias
 *
 */
public class RouteAliasObject implements Serializable {

    private final String alias;
    private final Class<? extends RouterLayout> layout;
    private final boolean absolute;

    /**
     * Creates a new instance using {@code alias} path, parent {@code layout}
     * and {@code absolute} calue.
     * 
     * @param alias
     *            a path
     * @param layout
     *            a parent layout
     * @param absolute
     *            whether the alias path us absolute
     */
    public RouteAliasObject(String alias, Class<? extends RouterLayout> layout,
            boolean absolute) {
        this.alias = alias;
        this.layout = layout;
        this.absolute = absolute;
    }

    /**
     * Creates a new instance with data based on provided {@code routeAlias}
     * 
     * @param routeAlias
     *            a route alias annotation
     */
    public RouteAliasObject(RouteAlias routeAlias) {
        this(routeAlias.value(), routeAlias.layout(), routeAlias.absolute());
    }

    /**
     * Gets the alias path.
     * 
     * @return the alias path
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Gets the parent router layout.
     * 
     * @return the router layout
     */
    public Class<? extends RouterLayout> getLayout() {
        return layout;
    }

    /**
     * Gets whether the alias path is absolute.
     * 
     * @return whether the alias path is absolute
     */
    public boolean isAbsolute() {
        return absolute;
    }

}
