/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    @NpmPackage(value = "@vaadin/horizontal-layout", version = "24.1.0")
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
