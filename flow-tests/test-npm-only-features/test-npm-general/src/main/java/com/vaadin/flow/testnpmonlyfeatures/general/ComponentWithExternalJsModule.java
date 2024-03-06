/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
