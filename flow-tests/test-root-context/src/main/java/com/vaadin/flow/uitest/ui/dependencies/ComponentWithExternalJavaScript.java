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

package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;

@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL)
@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL)
public class ComponentWithExternalJavaScript extends Div {
    public static final String SOME_RANDOM_EXTERNAL_JS_URL = "https://some-external-website.fi/another-js-module.js";
    public static final String SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL = "//some-external-website.fi/another-js-module.js";

    public ComponentWithExternalJavaScript() {
        add(new Text("A component with external JavaScript"));
    }
}
