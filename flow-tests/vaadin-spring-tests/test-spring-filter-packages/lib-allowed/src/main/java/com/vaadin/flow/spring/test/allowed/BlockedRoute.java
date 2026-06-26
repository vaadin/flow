/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.allowed;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/**
 * Blocked route in a jar package.
 */
@Route("blocked-route-in-jar")
public class BlockedRoute extends Div {

    public BlockedRoute() {
    }
}
