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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.Theme;

@JsModule("foo.js")
@Theme(themeClass = CustomTheme.class)
public class RouteLayoutComponent implements RouterLayout {
    @Override
    public Element getElement() {
        return null;
    }
}
