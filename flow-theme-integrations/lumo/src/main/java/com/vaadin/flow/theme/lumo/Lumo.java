/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.theme.lumo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Lumo component theme class implementation.
 *
 * @since 1.0
 */
@NpmPackage(value = "@vaadin/vaadin-lumo-styles", version = "1.6.1")
@JsModule("@vaadin/vaadin-lumo-styles/color.js")
@JsModule("@vaadin/vaadin-lumo-styles/typography.js")
@JsModule("@vaadin/vaadin-lumo-styles/sizing.js")
@JsModule("@vaadin/vaadin-lumo-styles/spacing.js")
@JsModule("@vaadin/vaadin-lumo-styles/style.js")
@JsModule("@vaadin/vaadin-lumo-styles/icons.js")
public class Lumo implements AbstractTheme {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/lumo/";
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return Collections.singletonList("<custom-style>"
                + "    <style include=\"lumo-color lumo-typography\"></style>"
                + "</custom-style>");
    }

    @Override
    @Deprecated
    public Map<String, String> getBodyAttributes(String variant) {
        return getHtmlAttributes(variant);
    }

    @Override
    public Map<String, String> getHtmlAttributes(String variant) {
        if (variant.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> attributes = new HashMap<>(1);
        switch (variant) {
        case LIGHT:
            attributes.put("theme", LIGHT);
            break;
        case DARK:
            attributes.put("theme", DARK);
            break;
        default:
            LoggerFactory.getLogger(Lumo.class.getName()).warn(
                    "Lumo theme variant not recognized: '{}'. Using no variant.",
                    variant);
        }
        return attributes;
    }
}