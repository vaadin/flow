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
    @Theme(value = HiddenTheme.class)
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
