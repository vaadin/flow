/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
