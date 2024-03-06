/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("js-injected-grand-child")
@JsModule("./JsInjectedGrandChild.js")
public class JsInjectedGrandChild extends PolymerTemplate<TemplateModel> {

    public JsInjectedGrandChild() {
        getElement().callJsFunction("greet");
        getElement().setProperty("bar", "foo");
    }

    @ClientCallable
    private void handleClientCall(String value) {
        getElement().setProperty("foo", value);
    }
}
