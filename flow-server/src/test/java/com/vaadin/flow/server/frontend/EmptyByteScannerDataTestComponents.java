/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Byte scanner will not be able to find anything from this class
 *
 */
public class EmptyByteScannerDataTestComponents {

    @JsModule("./common-js-file.js")
    @JavaScript("ExampleConnector.js")
    @CssImport(value = "./foo.css", id = "baz", include = "bar")
    @Theme(themeClass = HiddenTheme.class)
    public static class MainLayout extends Component {
    }

    @JsModule("@vaadin/vaadin-lumo-styles/icons.js")
    public static class HiddenTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "hidden-theme/";
        }

        @Override
        public List<String> getHeaderInlineContents() {
            return Collections
                    .singletonList("<custom-style>foo</custom-style>");
        }

    }
}
