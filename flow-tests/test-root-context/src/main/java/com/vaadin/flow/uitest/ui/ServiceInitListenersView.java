/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ServiceInitListenersView", layout = ViewTestLayout.class)
public class ServiceInitListenersView extends Div
        implements HasUrlParameter<Integer> {
    private static final String OPTIONAL_PARAMETER_LABEL_TEXT_PREFIX = "Before init count: ";
    private final Label optionalParameterLabel;

    public ServiceInitListenersView() {
        optionalParameterLabel = new Label();
        add(optionalParameterLabel);
        add(new Label(
                "Init count: " + TestingServiceInitListener.getInitCount()));
        add(new Label("Request count: "
                + TestingServiceInitListener.getRequestCount()));
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Integer parameter) {
        optionalParameterLabel
                .setText(OPTIONAL_PARAMETER_LABEL_TEXT_PREFIX + parameter);
    }
}
