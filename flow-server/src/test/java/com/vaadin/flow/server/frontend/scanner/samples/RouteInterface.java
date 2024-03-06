/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner.samples;

import com.vaadin.flow.component.dependency.CssImport;

@CssImport("frontend://styles/interface.css")
public interface RouteInterface {

    default void doSomething() {
        JsModuleComponent component = new JsModuleComponent();
        component.show();
    }
}
