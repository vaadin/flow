/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterEvent;

@Route(value = "npe")
public class NPETargetView extends AbstractDivView {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.rerouteToError(NullPointerException.class);
    }

}
