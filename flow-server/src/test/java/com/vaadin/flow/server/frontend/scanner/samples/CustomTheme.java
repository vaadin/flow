/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner.samples;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.theme.AbstractTheme;

@JsModule("theme-foo.js")
public class CustomTheme implements AbstractTheme {
    @Override
    public String getBaseUrl() {
        return null;
    }

    @Override
    public String getThemeUrl() {
        return null;
    }
}
