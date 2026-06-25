/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.frontend;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.frontend.DevBundleJsModuleView")
@JsModule("./test.ts")
@JsModule("./js/test.js")
public class DevBundleJsModuleView extends Div {
    static final String SPAN_ID = "test-js-module-frontend-span";

    public DevBundleJsModuleView() {
        Span span = new Span("Test JsModule with dev bundle");
        span.setId(SPAN_ID);
        add(span);

    }
}
