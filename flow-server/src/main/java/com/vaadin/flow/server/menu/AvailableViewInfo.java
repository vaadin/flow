/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.menu;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.vaadin.flow.router.MenuData;

/**
 * Represents a view configuration for use with a menu.
 *
 * @param title
 *            title of view
 * @param rolesAllowed
 *            logged in roles allowed for view
 * @param loginRequired
 *            requires login
 * @param route
 *            path string
 * @param lazy
 *            lazy loaded
 * @param register
 *            register view
 * @param menu
 *            menu item information
 * @param children
 *            view children
 * @param routeParameters
 *            view parameters
 * @param flowLayout
 *            if server layout should be used
 */
public record AvailableViewInfo(String title, String[] rolesAllowed,
        boolean loginRequired, String route, boolean lazy, boolean register,
        MenuData menu, List<AvailableViewInfo> children,
        @JsonProperty("params") Map<String, RouteParamType> routeParameters,
        boolean flowLayout) implements Serializable {

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AvailableViewInfo that = (AvailableViewInfo) o;
        return Objects.equals(title, that.title)
                && Arrays.equals(rolesAllowed, that.rolesAllowed)
                && Objects.equals(loginRequired, that.loginRequired)
                && Objects.equals(route, that.route)
                && Objects.equals(lazy, that.lazy)
                && Objects.equals(register, that.register)
                && Objects.equals(menu, that.menu)
                && Objects.equals(routeParameters, that.routeParameters)
                && Objects.equals(flowLayout, that.flowLayout);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, loginRequired, route, lazy, register,
                menu, routeParameters);
        result = 31 * result + Arrays.hashCode(rolesAllowed);
        return result;
    }

    @Override
    public String toString() {
        return "AvailableViewInfo{" + "title='" + title + '\''
                + ", rolesAllowed=" + Arrays.toString(rolesAllowed)
                + ", loginRequired=" + loginRequired + ", route='" + route
                + '\'' + ", lazy=" + lazy + ", register=" + register + ", menu="
                + menu + ", flowLayout=" + flowLayout + ", routeParameters="
                + routeParameters + '}';
    }

}
