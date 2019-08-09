package com.vaadin.flow.uitest.ui;

import java.util.Collections;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.BackButtonServerRoundTripView", layout = ViewTestLayout.class)
public class BackButtonServerRoundTripView extends Div implements HasUrlParameter<String> {

    static final String BUTTON_ID = "button";
    static final String QUERY_LABEL_ID = "query";

    private final Label queryLabel;

    public BackButtonServerRoundTripView() {
        queryLabel = new Label();
        queryLabel.setId(QUERY_LABEL_ID);
        add(queryLabel);

        NativeButton button = new NativeButton("Change query parameter",
                e -> UI.getCurrent().navigate(
                        "com.vaadin.flow.uitest.ui.BackButtonServerRoundTripView/1",
                        QueryParameters.simple(Collections.singletonMap("query", "bar"))));
        button.setId(BUTTON_ID);
        add(button);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @WildcardParameter String param) {
        queryLabel.setText(beforeEvent.getLocation().getQueryParameters().getQueryString());
    }
}
