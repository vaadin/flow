/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
     * RouteData constructor. This constructor doesn't support parameters. When
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
     * @param routeAliases
     *            list of aliases for this route
     * @throws IllegalArgumentException
     *             if parameters is not empty.
     */
    public RouteData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, List<Class<?>> parameters,
            Class<? extends Component> navigationTarget,
            List<RouteAliasData> routeAliases) {
        super(parentLayouts, template, parameters, navigationTarget);

        Collections.sort(routeAliases);
        this.routeAliases = Collections.unmodifiableList(routeAliases);
    }

    /**
     * RouteData constructor.
     *
     * @param parentLayouts
     *            route parent layout class chain
     * @param template
     *            full route template
     * @param parameters
     *            navigation target path parameters
     * @param navigationTarget
     *            route navigation target
     * @param routeAliases
     *            list of aliases for this route
     */
    public RouteData(List<Class<? extends RouterLayout>> parentLayouts,
            String template, Map<String, RouteParameterData> parameters,
            Class<? extends Component> navigationTarget,
            List<RouteAliasData> routeAliases) {
        super(parentLayouts, template, parameters, navigationTarget);

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
                + getTemplate() + '\'' + ", parameters=" + getRouteParameters()
                + ", navigationTarget=" + getNavigationTarget()
                + ", routeAliases=" + routeAliases + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RouteData) {
            RouteData other = (RouteData) obj;
            return other.getParentLayouts().equals(this.getParentLayouts())
                    && other.getTemplate().equals(this.getTemplate())
                    && other.getNavigationTarget().equals(getNavigationTarget())
                    && routeAliases.containsAll(other.routeAliases);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParentLayouts(), getTemplate(),
                getNavigationTarget(), routeAliases);
    }
}
