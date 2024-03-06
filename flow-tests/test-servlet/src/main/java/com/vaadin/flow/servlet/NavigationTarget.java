/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.servlet;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * Navigation target for which a servlet should automatically be registered.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Route("")
public class NavigationTarget extends Div {
    public NavigationTarget() {
        setText("Hello world");
        setId("navigationTarget");
    }
}
