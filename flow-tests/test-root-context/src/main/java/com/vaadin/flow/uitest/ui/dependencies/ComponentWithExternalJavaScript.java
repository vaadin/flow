/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
