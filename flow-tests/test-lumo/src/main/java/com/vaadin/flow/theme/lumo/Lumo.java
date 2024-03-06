/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
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

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

/**
 * Lumo component theme class implementation. Used for testing theming features.
 *
 * @since 1.0
 */
@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = TestVersion.VAADIN)
@NpmPackage(value = "@vaadin/polymer-legacy-adapter", version = TestVersion.VAADIN)
@JsModule("@vaadin/polymer-legacy-adapter/style-modules.js")
@NpmPackage(value = "@vaadin/vaadin-lumo-styles", version = TestVersion.VAADIN)
@JsModule("@vaadin/vaadin-lumo-styles/color.js")
@JsModule("@vaadin/vaadin-lumo-styles/typography.js")
@JsModule("@vaadin/vaadin-lumo-styles/sizing.js")
@JsModule("@vaadin/vaadin-lumo-styles/spacing.js")
@JsModule("@vaadin/vaadin-lumo-styles/style.js")
@JsModule("@vaadin/vaadin-lumo-styles/vaadin-iconset.js")
@JsModule("./lumo-includes.ts")
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
        return Collections.emptyList();
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
