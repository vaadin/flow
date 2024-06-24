/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.theme.AbstractTheme;

@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = "1.4.4")
public class MyTheme implements AbstractTheme {

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/";
    }
}
