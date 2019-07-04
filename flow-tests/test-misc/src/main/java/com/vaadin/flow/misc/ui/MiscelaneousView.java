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
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.misc.ui.MiscelaneousView.MyTheme;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

@Route(value = "")

// Import a test component that sets CSS properties.
@JsModule("./src/my-component-themed.js")
@HtmlImport("frontend://src/my-component-themed.html")

//`src/` in component above should be replaced by `theme/my-theme`
@Theme(MyTheme.class)
public class MiscelaneousView extends Div {
    
    public static class MyTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/my-theme";
        }
    }

    public MiscelaneousView() {
    }
}
