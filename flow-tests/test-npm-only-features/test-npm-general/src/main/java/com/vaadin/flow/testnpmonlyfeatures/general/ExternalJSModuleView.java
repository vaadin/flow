/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.general;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@JsModule(ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL)
@JsModule(ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL)
@Route(value = "com.vaadin.flow.testnpmonlyfeatures.general.ExternalJSModuleView", layout = ViewTestLayout.class)
public class ExternalJSModuleView extends Div {

    public ExternalJSModuleView() {
        NativeButton button = new NativeButton("Add component", event -> {
            ComponentWithExternalJsModule component = new ComponentWithExternalJsModule();
            component.setId("componentWithExternalJsModule");
            add(component);
        });
        button.setId("addComponentButton");
        add(button);
    }
}
