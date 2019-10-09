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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;

public class ExtraNodeTestComponents {

    @JsModule("@polymer/a.js")
    public class ExtraJsModuleComponent extends Component {
    }

    @JavaScript("./extra-javascript.js")
    public class ExtraJavaScriptComponent extends Component {
    }

    @CssImport(value = "./extra-css.css", themeFor = "extra-foo", include = "extra-bar")
    public static class ExtraCssImport extends Component {
    }

}
