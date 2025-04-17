/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.misc.ui;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.AbstractTheme;

@Route(value = "")

// Import a test component that sets CSS properties.
@JsModule("./src/my-component-themed.js")
// `src/` in component should be replaced by `legacyTheme/my-theme`
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
