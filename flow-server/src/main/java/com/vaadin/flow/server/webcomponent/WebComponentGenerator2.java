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
package com.vaadin.flow.server.webcomponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class for generating a client-side web component from a Java class.
 * <p>
 * Current implementation will create a Polymer 2 component that can be served
 * to the client.
 */
public class WebComponentGenerator2 {

    private static final String INDENTATION = "    ";

    private WebComponentGenerator2() {
    }

    private static String getTemplate() {
        try {
            return IOUtils.toString(
                    WebComponentGenerator2.class
                            .getResourceAsStream("webcomponent-template.html"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Couldn't load the template class", e);
        }
    }

    /**
     * Generate web component html/JS for given tag and class.
     *
     * @param uiElement
     *            string for finding the UI element on the client
     * @param tag
     *            web component tag
     * @param webComponentConfiguration
     *            web component class implementation
     * @param request
     *            a vaadin request
     * @return generated web component html/JS to be served to the client
     */
    public static String generateModule(String uiElement, String tag,
            WebComponentConfiguration<? extends Component> webComponentConfiguration,
            VaadinRequest request) {
        Set<PropertyData2<?>> propertyDataSet =
                webComponentConfiguration.getPropertyDataSet();

        Map<String, String> replacements = getReplacementsMap(uiElement, tag,
                propertyDataSet, getContextPath(request));

        String template = getTemplate();
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            template = template.replace("_" + replacement.getKey() + "_",
                    replacement.getValue());
        }
        return template;
    }

    static Map<String, String> getReplacementsMap(String uiElement, String tag,
            Set<PropertyData2<?>> propertyDataSet, String contextPath) {
        Map<String, String> replacements = new HashMap<>();

        replacements.put("TagDash", tag);
        replacements.put("TagCamel", SharedUtil
                .capitalize(SharedUtil.dashSeparatedToCamelCase(tag)));

        replacements.put("PropertyMethods", getPropertyMethods(
                propertyDataSet.stream().map(PropertyData2::getName)));

        replacements.put("Properties",
                getPropertyDefinitions(propertyDataSet));

        replacements.put("RootElement", uiElement);

        replacements.put("servlet_context", contextPath);

        return replacements;
    }

    private static String getContextPath(VaadinRequest request) {
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (!contextPath.isEmpty() && contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        return contextPath;
    }

    private static String getPropertyDefinitions(Set<PropertyData2<?>> properties) {
        JsonObject props = Json.createObject();

        for (PropertyData2<?> property : properties) {
            JsonObject prop = createPropertyDefinition(property);
            props.put(property.getName(), prop);
        }
        return props.toJson();
    }

    private static JsonObject createPropertyDefinition(PropertyData2<?> property) {
        JsonObject prop = Json.createObject();

        prop.put("type", property.getType().getSimpleName());

        if (property.getDefaultValue() != null) {
            String propertyValue = "value";
            if (property.getType() == Boolean.class) {
                prop.put(propertyValue, (Boolean) property.getDefaultValue());
            } else if (property.getType() == Double.class) {
                prop.put(propertyValue, (Double) property.getDefaultValue());
            } else if (property.getType() == Integer.class) {
                prop.put(propertyValue, (Integer) property.getDefaultValue());
            } else if (property.getType() == String.class) {
                prop.put(propertyValue, (String) property.getDefaultValue());
            } else if (JsonValue.class.isAssignableFrom(property.getType())) {
                prop.put(propertyValue, (JsonValue) property.getDefaultValue());
            }
            else {
                throw new UnsupportedPropertyType(String.format("%s " +
                        "is not a currently supported type for a Property. " +
                                "Please use %s instead.",
                        property.getType().getSimpleName(),
                        JsonValue.class.getSimpleName()));
            }
        }
        prop.put("observer", getSyncMethod(property.getName()));
        prop.put("notify", true);
        prop.put("reflectToAttribute", false);

        return prop;
    }

    private static String getSyncMethod(String property) {
        return "_sync_" + property;
    }

    private static String getPropertyMethods(Stream<String> properties) {
        StringBuilder methods = new StringBuilder();
        properties.forEach(property -> {
            methods.append(INDENTATION);
            methods.append(getSyncMethod(property));
            methods.append("(newValue, oldValue) { ");
            methods.append("this._sync('").append(property)
                    .append("', newValue);");
            methods.append("}\n");
        });
        return methods.toString();
    }
}
