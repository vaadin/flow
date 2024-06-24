/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.theme.material;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Material component theme class implementation.
 *
 * @since 1.2
 */
@HtmlImport("frontend://bower_components/vaadin-material-styles/color.html")
@HtmlImport("frontend://bower_components/vaadin-material-styles/typography.html")
@NpmPackage(value = "@vaadin/vaadin-material-styles", version = "1.3.2")
@JsModule("@vaadin/vaadin-material-styles/color.js")
@JsModule("@vaadin/vaadin-material-styles/typography.js")
public class Material implements AbstractTheme {
    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/material/";
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return Collections.singletonList("<custom-style>"
                + "    <style include=\"material-color-light material-typography\"></style>"
                + "</custom-style>");
    }

    @Override
    @Deprecated
    public Map<String, String> getBodyAttributes(String variant) {
        return getHtmlAttributes(variant);
    }

    @Override
    public Map<String, String> getHtmlAttributes(String variant) {
        switch (variant) {
        case LIGHT:
            return Collections.singletonMap("theme", LIGHT);
        case DARK:
            return Collections.singletonMap("theme", DARK);
        default:
            if (!variant.isEmpty()) {
                LoggerFactory.getLogger(getClass().getName()).warn(
                        "Material theme variant not recognized: '{}'. Using no variant.",
                        variant);
            }
            return Collections.emptyMap();
        }
    }
}
