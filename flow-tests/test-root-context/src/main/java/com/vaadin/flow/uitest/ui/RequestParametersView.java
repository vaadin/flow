/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.Collections;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RequestParametersView", layout = ViewTestLayout.class)
public class RequestParametersView extends Div implements BeforeEnterObserver {
    static final String REQUEST_PARAM_NAME = "testRequestParam";
    static final String NO_INPUT_TEXT = "No input";
    static final String REQUEST_PARAM_ID = "requestParamDisplayLabel";

    private final Label requestParamLabel;

    public RequestParametersView() {
        requestParamLabel = new Label(NO_INPUT_TEXT);
        requestParamLabel.setId(REQUEST_PARAM_ID);
        add(requestParamLabel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        requestParamLabel.setText(event.getLocation().getQueryParameters()
                .getParameters()
                .getOrDefault(REQUEST_PARAM_NAME, Collections.emptyList())
                .stream().findFirst().orElse(NO_INPUT_TEXT));

    }
}
