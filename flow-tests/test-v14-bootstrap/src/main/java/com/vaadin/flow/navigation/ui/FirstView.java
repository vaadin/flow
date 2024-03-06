/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigation.ui;

import java.util.Collections;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route(value = "first")
public class FirstView extends Div implements HasUrlParameter<String> {

    static final String PARAM_NAV_BUTTON_ID = "firstViewPramNavButton";
    static final String NAV_BUTTON_ID = "firstViewNavButton";
    static final String QUERY_LABEL_ID = "query";

    private final Label queryLabel;

    public FirstView() {
        queryLabel = new Label();
        queryLabel.setId(QUERY_LABEL_ID);
        add(queryLabel);

        NativeButton button = new NativeButton("Change query parameter",
                e -> UI.getCurrent().navigate("first/1", QueryParameters
                        .simple(Collections.singletonMap("query", "bar"))));
        button.setId(PARAM_NAV_BUTTON_ID);
        add(button);

        button = new NativeButton("Change view",
                e -> UI.getCurrent().navigate("second"));
        button.setId(NAV_BUTTON_ID);
        add(button);

    }

    @Override
    public void setParameter(BeforeEvent beforeEvent,
            @OptionalParameter String param) {
        queryLabel.setText(beforeEvent.getLocation().getQueryParameters()
                .getQueryString());
    }
}
