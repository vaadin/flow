/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RouteAndQueryParametersView", layout = ViewTestLayout.class)
public class RouteAndQueryParametersView extends Div
        implements HasUrlParameter<Integer> {
    static final String REQUEST_PARAM_NAME = "testRequestParam";

    private final Paragraph paramView;

    public RouteAndQueryParametersView() {
        paramView = new Paragraph("No input");
        paramView.setId("paramView");
        add(paramView);

        NativeButton nativeButton = new NativeButton("Navigate with both");
        nativeButton.setId("both");
        nativeButton.addClickListener(e -> {
            UI.getCurrent().navigate(RouteAndQueryParametersView.class, 5,
                    QueryParameters.of("foo", "bar"));
        });
        add(nativeButton);

        NativeButton withQueryParametersOnly = new NativeButton(
                "Navigate with qp");
        withQueryParametersOnly.setId("qponly");
        withQueryParametersOnly.addClickListener(e -> {
            UI.getCurrent().navigate(RouteAndQueryParametersView.class,
                    QueryParameters.of("foo", "bar"));
        });
        add(withQueryParametersOnly);

    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Integer parameter) {
        String queryString = event.getLocation().getQueryParameters()
                .getQueryString();
        paramView.setText("route parameter: " + parameter + ", query string:"
                + queryString);

    }
}
