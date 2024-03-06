/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("navigation-exception")
public class NavigationExceptionTargetView extends Div {

    public NavigationExceptionTargetView() {
        throw new NavigationException();
    }
}
