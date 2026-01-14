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
package com.vaadin.flow.prodbuild;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.prodbuild.ThemeComponentsCssView")
public class ThemeComponentsCssView extends Div {

    public ThemeComponentsCssView() {
        add(new MyComponent());
    }

    // Same dependency as in test prod-bundle.
    // If no <theme>/components/vaadin-horizontal-layout.css files are present,
    // the bundle will not be rebuilt.
    @JsModule("@vaadin/horizontal-layout")
    @NpmPackage(value = "@vaadin/horizontal-layout", version = "25.0.0-rc1")
    @Tag("vaadin-horizontal-layout")
    public static class MyComponent extends Component {
        public MyComponent() {
            getElement()
                    .appendChild(ElementFactory.createDiv("Specific Theme"));
            getElement()
                    .appendChild(ElementFactory.createDiv("Reusable Theme"));
            getElement().appendChild(ElementFactory.createDiv("Other theme"));
        }
    }

}
