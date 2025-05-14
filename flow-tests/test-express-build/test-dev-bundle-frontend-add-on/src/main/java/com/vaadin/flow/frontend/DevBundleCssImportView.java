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
package com.vaadin.flow.frontend;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.frontend.DevBundleCssImportView")
@CssImport("./styles/my-styles.css")
@CssImport("./styles/my-sass.scss")
@NpmPackage(value = "sass-embedded", version = "1.80.6")
public class DevBundleCssImportView extends Div {
    static final String MY_COMPONENT_ID = "test-css-import-meta-inf-resources-span";

    static final String SPAN_ID = "test-css-import-frontend-span";

    public DevBundleCssImportView() {
        Span span = new Span("Test CssImport with dev bundle");
        span.setId(SPAN_ID);
        add(span);

        MyComponent myComponent = new MyComponent();
        myComponent.setId(MY_COMPONENT_ID);
        add(myComponent);
    }
}
