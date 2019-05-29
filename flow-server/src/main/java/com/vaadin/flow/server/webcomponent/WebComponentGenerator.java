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
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.JsonValue;

/**
 * Generates a client-side web component from a Java class.
 * <p>
 * Current implementation will create a Polymer 2 component that can be served
 * to the client.
 *
 * @author Vaadin Ltd.
 */
public class WebComponentGenerator {
    private static final String HTML_TEMPLATE = "webcomponent-template.html";
    private static final String JS_TEMPLATE = "webcomponent-template.js";
    private static final String SCRIPT_TEMPLATE = "webcomponent-script-template.js";

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
            Class<? extends WebComponentExporter<? extends Component>> exporterClass,
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

        replacements.put("AttributeChange",
                getAttributeChange(propertyDataSet));
        replacements.put("PropertyMethods",
                getPropertyMethods(propertyDataSet));
        replacements.put("PropertyDefaults",
                getPropertyDefaults(propertyDataSet));
        replacements.put("PropertyValues", getPropertyValues(propertyDataSet));

        replacements.put("frontend_resources", frontendURI);

        replacements.put("ui_import",
                generateUiImport
                ? "<link rel='import' href='web-component-ui.html'>"
                        : "");

        return replacements;
    }

    private static String getPropertyMethods(Set<PropertyData<?>> properties) {
        StringBuilder setters = new StringBuilder();
        for (PropertyData<?> property : properties) {
            setters.append(createPropertySetterGetter(property));
        }
        return setters.toString();
    }

    private static String getPropertyDefaults(Set<PropertyData<?>> properties) {
        StringBuilder setters = new StringBuilder();
        for (PropertyData<?> property : properties) {
            setters.append(createPropertyDefault(property));
        }
        return setters.toString();
    }

    private static String getPropertyValues(Set<PropertyData<?>> properties) {
        if (properties.isEmpty()) {
            return "{}";
        }
        StringBuilder sync = new StringBuilder();
        sync.append("{");
        for (PropertyData<?> property : properties) {
            sync.append("'").append(property.getName()).append("': ")
            .append("this['").append(property.getName()).append("'], ");
        }
        sync.delete(sync.length() - 1, sync.length());
        sync.append("}");
        return sync.toString();
    }

    private static String getAttributeChange(Set<PropertyData<?>> properties) {
        // if (attribute == "show") {
        // this.show = this._deserializeValue(value, Boolean);
        // }

        StringBuilder sync = new StringBuilder();
        for (PropertyData<?> property : properties) {
            sync.append("if (attribute === '")
            .append(property.getAttributeName()).append("') {\n")
            .append("  this['").append(property.getName())
            .append("'] = this._deserializeValue(value, ")
            .append(property.getJSType()).append(");\n").append("}\n");
        }
        return sync.toString();
    }

    private static String createPropertyDefault(PropertyData<?> property) {

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
            ;

        } else {
            throw new UnsupportedPropertyTypeException(String.format(
                    "%s is not a currently supported type for a Property."
                            + " Please use %s instead.",
                            property.getType().getSimpleName(),
                            JsonValue.class.getSimpleName()));
        }

        // if (this.hasOwnProperty('foo')) {
        /*
         * when a property has been set before the element is upgraded, it needs
         * to be deleted so the getter/setters are used
         */
        // this['_foo'] = this['foo'];
        // delete this['foo'];
        // } else {
        // this['_foo'] = 'defaultValue'
        // }
        StringBuilder builder = new StringBuilder();
        builder.append("if (this.hasOwnProperty('").append(property.getName())
        .append("')) {\n");
        builder.append("  this['_").append(property.getName())
        .append("'] = this['").append(property.getName())
        .append("'];\n");
        builder.append("  delete this['").append(property.getName())
        .append("'];\n");
        if (!"undefined".equals(value)) {
            builder.append("} else {\n");
            builder.append("  this['_").append(property.getName())
                    .append("']=" + value + ";\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static String createPropertySetterGetter(PropertyData<?> property) {
        // set someProperty(value) {
        // if (this._someProperty === value) return;
        // this._someProperty = value;
        // this._sync('someProperty',value);
        // var eventDetails = { value: value };
        // var eventName = 'some-property'+'-changed';
        // this.dispatchEvent(new CustomEvent(eventName, eventDetails));
        // }
        // get someProperty() {
        // return this._someProperty;
        // }
        StringBuilder builder = new StringBuilder();
        builder.append("set ['").append(property.getName())
        .append("'](value) {").append("\n");
        builder.append("if (this['_").append(property.getName())
        .append("'] === value) return;\n");
        builder.append("this['_").append(property.getName());
        builder.append("'] = value;\n");
        builder.append("this._sync('").append(property.getName())
        .append("', value);\n");
        builder.append("var eventDetails = { value: value };\n");
        builder.append("var eventName = '")
        .append(SharedUtil.camelCaseToDashSeparated(property.getName()))
        .append("'+'-changed';\n");
        builder.append(
                "this.dispatchEvent(new CustomEvent(eventName, eventDetails));\n");

        builder.append("}\n");

        builder.append("get ['").append(property.getName()).append("']() {")
        .append("\n");
        builder.append("return this['_").append(property.getName())
        .append("'];\n");
        builder.append("}\n");
        return builder.toString();
    }

}
