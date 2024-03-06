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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;

@ParentLayout(NavigationExceptionLayout.class)
public class NavigationExceptionHandler extends Div
        implements HasErrorParameter<NavigationException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NavigationException> parameter) {
        return 500;
    }
}
