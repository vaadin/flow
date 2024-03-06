/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.util.List;
import java.util.Map;
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
     * AliasData constructor. This constructor doesn't support parameters. When
     * a non empty List is provided {@link IllegalArgumentException} is raised.
     *
     * @param parentLayouts
     *            route parent layout class chain
     * @param template
     *            full route template
     * @param parameters
     *            supports only null or empty list. If a non empty list is
     *            passed and {@link IllegalArgumentException} is raised.
     * @param navigationTarget
     *            route navigation target
     * @throws IllegalArgumentException
     *             if parameters is not empty.
     */
    public RouteAliasData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget) {
        super(parentLayouts, template, parameters, navigationTarget);
    }

    /**
     * AliasData constructor.
     *
     * @param parentLayouts
     *            route parent layout class chain
     * @param template
     *            full route template
     * @param parameters
     *            navigation target path parameters
     * @param navigationTarget
     *            route navigation target
     */
    public RouteAliasData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, Map<String, RouteParameterData> parameters,
            Class<? extends Component> navigationTarget) {
        super(parentLayouts, template, parameters, navigationTarget);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteAliasData) {
            RouteAliasData other = (RouteAliasData) obj;
            return other.getParentLayouts().equals(this.getParentLayouts())
                    && other.getTemplate().equals(this.getTemplate())
                    && other.getNavigationTarget()
                            .equals(getNavigationTarget());
        }
        return false;
    }

    @Override
    public String toString() {
        return "RouteData{" + "parentLayout=" + getParentLayout() + ", url='"
                + getTemplate() + '\'' + ", parameters=" + getRouteParameters()
                + ", navigationTarget=" + getNavigationTarget() + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParentLayouts(), getTemplate(),
                getNavigationTarget());
    }
}
