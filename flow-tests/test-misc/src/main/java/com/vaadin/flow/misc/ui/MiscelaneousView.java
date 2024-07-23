/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.misc.ui.MiscelaneousView.MyTheme;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

@Route(value = "")

// Import a test component that sets CSS properties.
@JsModule("./src/my-component-themed.js")

// `src/` in component above should be replaced by `theme/my-theme`
@Theme(MyTheme.class)
@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
public class MiscelaneousView extends Div {

    public static final String TEST_VIEW_ID = "MiscellaneousView";

    public static class MyTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "legacyTheme/my-theme";
        }

        @Override
        public List<String> getHeaderInlineContents() {
            return Collections
                    .singletonList("<custom-style>\n <style>\n   html {\n"
                            + "      font-size: 20px;\n  color:red;  }\n <style>\n </custom-style>");
        }
    }

    public MiscelaneousView() {
        setId(TEST_VIEW_ID);
    }
}
