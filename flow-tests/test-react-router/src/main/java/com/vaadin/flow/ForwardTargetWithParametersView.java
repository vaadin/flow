/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.ForwardTargetWithParametersView")
public class ForwardTargetWithParametersView extends Div
        implements HasUrlParameter<String> {

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        add(new Span("setParameter"));
    }
}
