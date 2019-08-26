/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.testnpmonlyfeatures.general;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;

@JsModule(ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL)
@JsModule(ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL)
@JsModule("./" + ComponentWithExternalJsModule.NON_EXTERNAL_JS_MODULE_NAME)
public class ComponentWithExternalJsModule extends Div {
    public static final String SOME_RANDOM_EXTERNAL_JS_URL = "https://some-external-website.fi/another-js-module.js";
    public static final String SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL = "//some-external-website.fi/another-js-module.js";
    public static final String NON_EXTERNAL_JS_MODULE_NAME = "my-component.js";

    public ComponentWithExternalJsModule() {
        add(new Text("A component with external JsModule"));
    }
}
