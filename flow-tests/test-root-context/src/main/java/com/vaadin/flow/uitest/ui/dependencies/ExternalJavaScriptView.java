/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL)
@JavaScript(ComponentWithExternalJavaScript.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL)
@Route(value = "com.vaadin.flow.uitest.ui.dependencies.ExternalJavaScriptView", layout = ViewTestLayout.class)
public class ExternalJavaScriptView extends Div {

    public ExternalJavaScriptView() {
        NativeButton button = new NativeButton("Add component", event -> {
            ComponentWithExternalJavaScript component = new ComponentWithExternalJavaScript();
            component.setId("componentWithExternalJavaScript");
            add(component);
        });
        button.setId("addComponentButton");
        add(button);
    }
}
