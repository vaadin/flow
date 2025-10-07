/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import com.vaadin.flow.internal.JacksonUtils;
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
 * @param detail
 *            additional information to be used in the menu, encoded in JSON
 *            format
 */
public record AvailableViewInfo(String title, String[] rolesAllowed,
        boolean loginRequired, String route, boolean lazy, boolean register,
        MenuData menu, List<AvailableViewInfo> children,
        @JsonProperty("params") Map<String, RouteParamType> routeParameters,
        boolean flowLayout,
        @JsonDeserialize(using = DetailDeserializer.class) @JsonSerialize(using = DetailSerializer.class) String detail)
        implements
            Serializable {

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
                && Objects.equals(flowLayout, that.flowLayout)
                && Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, loginRequired, route, lazy, register,
                menu, routeParameters, detail);
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
                + routeParameters + ", detail=" + detail + '}';
    }

    public static class DetailDeserializer extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }
            JsonNode node = p.readValueAsTree();
            return node.toString();
        }
    }

    public static class DetailSerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializationContext serializers) {
            if (value == null) {
                gen.writeNull();
                return;
            }
            JsonNode node = JacksonUtils.readTree(value);
            gen.writePOJO(node);
        }
    }

}
