/*
 * Copyright 2000-2024 Vaadin Ltd.
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
 * @param rolesAllowed
 * @param loginRequired
 * @param route
 * @param lazy
 * @param register
 * @param menu
 * @param children
 * @param routeParameters
 */
public record AvailableViewInfo(String title, String[] rolesAllowed,
                                boolean loginRequired, String route, boolean lazy,
                                boolean register, MenuData menu,
                                List<AvailableViewInfo> children, @JsonProperty(
        "params") Map<String, RouteParamType> routeParameters) implements Serializable {

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
                && Objects.equals(routeParameters, that.routeParameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, loginRequired, route, lazy, register, menu, routeParameters);
        result = 31 * result + Arrays.hashCode(rolesAllowed);
        return result;
    }

    @Override
    public String toString() {
        return "AvailableViewInfo{" + "title='" + title
                + '\'' + ", rolesAllowed=" + Arrays.toString(rolesAllowed)
                + ", loginRequired=" + loginRequired
                + ", route='" + route + '\''
                + ", lazy=" + lazy
                + ", register=" + register
                + ", menu=" + menu
                + ", routeParameters=" + routeParameters + '}';
    }

}
