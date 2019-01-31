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
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;
import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentMethod;
import com.vaadin.flow.component.webcomponent.WebComponentProperty;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Class for generating a client-side web component from a Java class.
 * <p>
 * Current implementation will create a Polymer 2 component that can be served
 * to the client.
 */
public class WebComponentGenerator {

    private static final String INDENTATION = "    ";
    private static final String template = getTemplate();

    private WebComponentGenerator() {
    }

    private static String getTemplate() {
        try {
            return IOUtils.toString(WebComponentGenerator.class
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
     *         string for finding the UI element on the client
     * @param tag
     *         web component tag
     * @param webComponentClass
     *         web component class implementation
     * @param instantiator
     *         class instantiator implementation
     * @return generated web component html/JS to be served to the client
     */
    public static String generateModule(String uiElement, String tag,
            Class<? extends Component> webComponentClass,
            Instantiator instantiator) {
        Set<PropertyData> webComponentProperties = getPropertyData(
                webComponentClass, instantiator);

        Map<String, String> replacements = getReplacementsMap(uiElement, tag,
                webComponentProperties);

        String template = getTemplate();
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            template = template.replace("_" + replacement.getKey() + "_",
                    replacement.getValue());
        }
        return template;
    }

    static Set<PropertyData> getPropertyData(
            Class<? extends Component> webComponentClass,
            Instantiator instantiator) {
        Set<PropertyData> webComponentProperties = new HashSet<>();

        webComponentProperties
                .addAll(getMethodPropertiesWithDefaults(webComponentClass));
        webComponentProperties
                .addAll(getFieldProperties(webComponentClass, instantiator));
        return webComponentProperties;
    }

    static Map<String, String> getReplacementsMap(String uiElement, String tag,
            Set<PropertyData> webComponentProperties) {
        Map<String, String> replacements = new HashMap<>();

        replacements.put("TagDash", tag);
        replacements.put("TagCamel", SharedUtil
                .capitalize(SharedUtil.dashSeparatedToCamelCase(tag)));

        replacements.put("PropertyMethods", getPropertyMethods(
                webComponentProperties.stream().map(PropertyData::getName)));

        replacements.put("Properties",
                getPropertyDefinitions(webComponentProperties));

        replacements.put("RootElement", uiElement);

        return replacements;
    }

    private static Set<PropertyData> getFieldProperties(
            Class<? extends Component> webComponentClass,
            Instantiator instantiator) {

        List<Field> propertyFields = getPropertyFields(webComponentClass);

        Set<PropertyData> properties = new HashSet<>();
        if (propertyFields.isEmpty()) {
            return properties;
        }

        Component wc = instantiator.getOrCreate(webComponentClass);

        // Add fields in order so any overriding child field is added first
        // making the parent field not add their property data.
        for (Field field : propertyFields) {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Class<?> typeClass;
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                typeClass = GenericTypeReflector
                        .erase(((ParameterizedType) type)
                                .getActualTypeArguments()[0]);
            } else {
                typeClass = GenericTypeReflector.erase(field.getType());
            }
            try {
                Object propertyValue = ((WebComponentProperty) field.get(wc))
                        .get();
                properties.add(new PropertyData(field.getName(), typeClass,
                        propertyValue == null ?
                                null :
                                propertyValue.toString()));
            } catch (IllegalAccessException e) {
                throw new PropertyReadException(
                        "Failed to get the property value for field '" + field
                                .getName() + "'", e);
            } finally {
                field.setAccessible(accessible);
            }
        }

        return properties;
    }

    /**
     * Get all {@link WebComponentProperty} fields for target class and parents.
     * Fields are set in the order of child first parent last.
     *
     * @param webComponentClass
     *         class to get fields for
     * @return {@link WebComponentProperty} fields
     */
    private static List<Field> getPropertyFields(Class<?> webComponentClass) {
        List<Field> fields = new ArrayList<>();

        // Add all WebComponent fields for this class
        fields.addAll(Stream.of(webComponentClass.getDeclaredFields())
                .filter(field -> WebComponentProperty.class
                        .isAssignableFrom(field.getType()))
                .collect(Collectors.toSet()));

        // Add the parent fields after our fields
        if (webComponentClass.getSuperclass() != null) {
            fields.addAll(getPropertyFields(webComponentClass.getSuperclass()));
        }

        return fields;
    }

    private static Set<PropertyData> getMethodPropertiesWithDefaults(
            Class<?> webComponentClass) {

        Set<PropertyData> properties = new HashSet<>();

        if (webComponentClass.getSuperclass() != null) {
            properties.addAll(getMethodPropertiesWithDefaults(
                    webComponentClass.getSuperclass()));
        }

        Stream.of(webComponentClass.getDeclaredMethods())
                .filter(method -> method
                        .isAnnotationPresent(WebComponentMethod.class))
                .forEach(method -> {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    WebComponentMethod annotation = method
                            .getAnnotation(WebComponentMethod.class);
                    PropertyData property = new PropertyData(annotation.value(),
                            parameterTypes[0], annotation.initialValue());
                    if (!properties.add(property)) {
                        properties.remove(property);
                        properties.add(property);
                    }
                });

        return properties;
    }

    private static String getPropertyDefinitions(Set<PropertyData> properties) {
        JsonObject props = Json.createObject();

        for (PropertyData property : properties) {
            JsonObject prop = createPropertyDefinition(property);
            props.put(property.getName(), prop);
        }
        return props.toJson();
    }

    private static JsonObject createPropertyDefinition(PropertyData property) {
        JsonObject prop = Json.createObject();

        prop.put("type", property.getType().getSimpleName());

        Class<?> convertedType = ReflectTools
                .convertPrimitiveType(property.getType());
        if (property.getInitialValue() != null) {
            String propertyValue = "value";
            if (convertedType == Boolean.class) {
                prop.put(propertyValue, (Boolean) convertedType
                        .cast(Boolean.valueOf(property.getInitialValue())));
            } else if (convertedType == Double.class) {
                prop.put(propertyValue, (Double) convertedType
                        .cast(Double.valueOf(property.getInitialValue())));
            } else if (convertedType == Integer.class) {
                prop.put(propertyValue, (Integer) convertedType
                        .cast(Integer.valueOf(property.getInitialValue())));
            } else {
                prop.put(propertyValue, property.getInitialValue());
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
