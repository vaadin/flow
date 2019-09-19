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

import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;

/**
 * Route data for an 'alias' route. An alias route is a route that will get
 * another path than this when it is resolved. The same target will still be
 * resolved for this path, but the parent layouts may differ.
 * <p>
 * Only for read as data is immutable.
 *
 * @since 1.3
 */
public class RouteAliasData extends RouteBaseData<RouteAliasData> {

    /**
     * AliasData constructor.
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
    public RouteAliasData(List<Class<? extends RouterLayout>> parentLayouts,
            String url, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        super(parentLayouts, url, parameters, navigationTarget);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteAliasData) {
            RouteAliasData other = (RouteAliasData) obj;
            return other.getParentLayouts().equals(this.getParentLayouts())
                    && other.getUrl().equals(this.getUrl()) && other
                    .getNavigationTarget().equals(getNavigationTarget());
        }
        return false;
    }

    @Override
    public String toString() {
        return "RouteData{" + "parentLayout=" + getParentLayout() + ", url='"
                + getUrl() + '\'' + ", parameters=" + getParameters()
                + ", navigationTarget=" + getNavigationTarget() + '}';
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(getParentLayouts(), getUrl(), getNavigationTarget());
    }
}
