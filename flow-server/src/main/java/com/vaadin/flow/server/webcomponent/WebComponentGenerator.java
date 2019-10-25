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
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Generates a client-side web component from a Java class.
 * <p>
 * Current implementation will create a Polymer 2 component that can be served
 * to the client.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentGenerator {
    private static final String TOKEN_DEFAULT_VALUE = "_DefaultValue_";
    private static final String TOKEN_JS_TYPE = "_JSType_";
    private static final String TOKEN_ATTRIBUTE_NAME = "_AttributeName_";
    private static final String TOKEN_CHANGE_EVENT_NAME = "_ChangeEventName_";
    private static final String TOKEN_PROPERTY_NAME = "_PropertyName_";
    private static final String HTML_TEMPLATE = "webcomponent-template.html";
    private static final String JS_TEMPLATE = "webcomponent-template.js";
    private static final String SCRIPT_TEMPLATE = "webcomponent-script-template.js";
    private static final String CODE_PROPERTY_DEFAULT = "webcomponent-property-default.js";
    private static final String CODE_PROPERTY_VALUES = "webcomponent-property-values.js";
    private static final String CODE_ATTRIBUTE_CHANGE = "webcomponent-attribute-change.js";
    private static final String CODE_PROPERTY_METHODS = "webcomponent-property-methods.js";

    private WebComponentGenerator() {
    }

    private static String getStringResource(String name) {
        try {
            return IOUtils.toString(
                    WebComponentGenerator.class.getResourceAsStream(name),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Couldn't load string resource '" + name + "'!", e);
        }
    }

    private static String getTemplate(boolean compatibilityMode) {
        String templateHead;
        if (compatibilityMode) {
            templateHead = getStringResource(HTML_TEMPLATE);
        } else {
            templateHead = getStringResource(JS_TEMPLATE);
        }
        String scriptTemplate = getStringResource(SCRIPT_TEMPLATE);
        return templateHead.replace("_script_template_", scriptTemplate);
    }

    /**
     * Generate web component html/JS for given exporter class.
     *
     * @param exporterClass
     *            web component exporter class, not {@code null}
     * @param frontendURI
     *            the frontend resources URI, not {@code null}
     * @param compatibilityMode
     *            {@code true} to generate Polymer2 template, {@code false} to
     *            generate Polymer3 template
     * @return generated web component html/JS to be served to the client
     */
    public static String generateModule(
            Class<? extends ExportsWebComponent<? extends Component>> exporterClass,
                    String frontendURI, boolean compatibilityMode) {
        Objects.requireNonNull(exporterClass);
        Objects.requireNonNull(frontendURI);

        WebComponentConfiguration<? extends Component> config = new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporterClass);

        return generateModule(config, frontendURI, false, compatibilityMode);
    }

    /**
     * Generate web component html/JS for given tag and class.
     *
     * @param webComponentConfiguration
     *            web component class implementation, not {@code null}
     * @param frontendURI
     *            the frontend resources URI, not {@code null}
     * @param compatibilityMode
     *            {@code true} to generate Polymer2 template, {@code false} to
     *            generate Polymer3 template
     * @return generated web component html/JS to be served to the client
     */
    public static String generateModule(
            WebComponentConfiguration<? extends Component> webComponentConfiguration,
            String frontendURI, boolean compatibilityMode) {
        Objects.requireNonNull(webComponentConfiguration);
        Objects.requireNonNull(frontendURI);

        return generateModule(webComponentConfiguration, frontendURI, true,
                compatibilityMode);
    }

    private static String generateModule(
            WebComponentConfiguration<? extends Component> webComponentConfiguration,
            String frontendURI, boolean generateUiImport,
            boolean compatibilityMode) {
        Objects.requireNonNull(webComponentConfiguration);
        Objects.requireNonNull(frontendURI);

        Set<PropertyData<?>> propertyDataSet = webComponentConfiguration
                .getPropertyDataSet();

        Map<String, String> replacements = getReplacementsMap(
                webComponentConfiguration.getTag(), propertyDataSet,
                frontendURI, generateUiImport);

        String template = getTemplate(compatibilityMode);
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            template = template.replace("_" + replacement.getKey() + "_",
                    replacement.getValue());
        }
        return template;
    }

    static Map<String, String> getReplacementsMap(String tag,
            Set<PropertyData<? extends Serializable>> propertyDataSet,
            String frontendURI, boolean generateUiImport) {
        Map<String, String> replacements = new HashMap<>();

        replacements.put("TagDash", tag);
        replacements.put("TagCamel", SharedUtil
                .capitalize(SharedUtil.dashSeparatedToCamelCase(tag)));

        replacements.put("AttributeChange", getAttributeChange(
                getStringResource(CODE_ATTRIBUTE_CHANGE), propertyDataSet));
        replacements.put("PropertyMethods", getPropertyMethods(
                getStringResource(CODE_PROPERTY_METHODS), propertyDataSet));
        replacements.put("PropertyDefaults", getPropertyDefaults(
                getStringResource(CODE_PROPERTY_DEFAULT), propertyDataSet));
        replacements.put("PropertyValues", getPropertyValues(
                getStringResource(CODE_PROPERTY_VALUES), propertyDataSet));

        replacements.put("frontend_resources", frontendURI);

        replacements.put("ui_import",
                generateUiImport
                ? "<link rel='import' href='web-component-ui.html'>"
                        : "");

        return replacements;
    }

    private static String getPropertyMethods(String codePropertyMethods,
            Set<PropertyData<?>> properties) {
        StringBuilder setters = new StringBuilder();
        for (PropertyData<?> property : properties) {
            setters.append(
                    createPropertySetterGetter(codePropertyMethods, property));
        }
        return setters.toString();
    }

    private static String getPropertyDefaults(String codePropertyDefault,
            Set<PropertyData<?>> properties) {
        StringBuilder setters = new StringBuilder();
        for (PropertyData<?> property : properties) {
            setters.append(
                    createPropertyDefault(codePropertyDefault, property));
        }
        return setters.toString();
    }

    private static String getPropertyValues(String codePropertyValues,
            Set<PropertyData<?>> properties) {
        if (properties.isEmpty()) {
            return "{}";
        }
        StringBuilder sync = new StringBuilder();
        sync.append("{");
        for (PropertyData<?> property : properties) {
            sync.append(codePropertyValues.replace(TOKEN_PROPERTY_NAME,
                    property.getName()));
            sync.append(",");
        }
        sync.delete(sync.length() - 1, sync.length());
        sync.append("}");
        return sync.toString();
    }

    private static String getAttributeChange(String codeAttributeChange,
            Set<PropertyData<?>> properties) {
        StringBuilder sync = new StringBuilder();
        for (PropertyData<?> property : properties) {
            sync.append(codeAttributeChange
                    .replace(TOKEN_ATTRIBUTE_NAME, getAttributeName(property))
                    .replace(TOKEN_PROPERTY_NAME, property.getName())
                    .replace(TOKEN_JS_TYPE, getJSTypeName(property)));
        }
        return sync.toString();
    }

    private static String createPropertyDefault(String code,
            PropertyData<?> property) {
        // Note about the JS code:
        // If a property has been set before the element is upgraded, it needs
        // to be deleted so the getter/setters are used

        return code.replace(TOKEN_PROPERTY_NAME, property.getName())
                .replace(TOKEN_DEFAULT_VALUE, getDefaultJsValue(property));
    }

    private static String getDefaultJsValue(PropertyData<?> property) {
        String value;
        if (property.getDefaultValue() == null) {
            value = "undefined";
        } else if (property.getType() == Boolean.class) {
            value = String.valueOf((property.getDefaultValue()));
        } else if (property.getType() == Double.class) {
            value = String.valueOf((property.getDefaultValue()));
        } else if (property.getType() == Integer.class) {
            value = String.valueOf((property.getDefaultValue()));
        } else if (property.getType() == String.class) {
            value = "'" + ((String) property.getDefaultValue()).replaceAll("'",
                    "\\'") + "'";
        } else if (JsonValue.class.isAssignableFrom(property.getType())) {
            value = ((JsonValue) property.getDefaultValue()).toJson();
        } else {
            throw new UnsupportedPropertyTypeException(String.format(
                    "%s is not a currently supported type for a Property."
                            + " Please use %s instead.",
                            property.getType().getSimpleName(),
                            JsonValue.class.getSimpleName()));
        }
        if (value == null) {
            value = "null";
        }

        return value;
    }

    private static String createPropertySetterGetter(String codePropertyMethods,
            PropertyData<?> property) {
        return codePropertyMethods
                .replace(TOKEN_PROPERTY_NAME, property.getName())
                .replace(TOKEN_CHANGE_EVENT_NAME,
                        getAttributeName(property) + "-changed");
    }

    /**
     * Gets {@link com.vaadin.flow.server.webcomponent.PropertyData} name used
     * when setting its value through an attribute.
     *
     * @return the attribute name used for setting the value
     */
    private static String getAttributeName(PropertyData<?> propertyData) {
        return SharedUtil.camelCaseToDashSeparated(propertyData.getName());
    }

    /**
     * Gets JavaScript type name for {@link com.vaadin.flow.server.webcomponent.PropertyData}
     * for usage in generated JavaScript code.
     *
     * @return the type for JS
     */
    private static String getJSTypeName(PropertyData<?> propertyData) {
        if (propertyData.getType() == Boolean.class) {
            return "Boolean";
        } else if (propertyData.getType() == Double.class || propertyData.getType() == Integer.class) {
            return "Number";
        } else if (propertyData.getType() == String.class) {
            return "String";
        } else if (JsonArray.class.isAssignableFrom(propertyData.getType())) {
            return "Array";
        } else if (JsonValue.class.isAssignableFrom(propertyData.getType())) {
            return "Object";
        } else {
            throw new IllegalStateException("Unsupported type: " + propertyData.getType());
        }
    }

}
