package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SetParameterForwardToView", layout = ViewTestLayout.class)
public class SetParameterForwardToView extends Div
        implements HasUrlParameter<String>, AfterNavigationObserver {

    static final String LOCATION_ID = "location";
    static final String PARAMETER_ID = "parameter";

    private final Div location;
    private final Div param;

    public SetParameterForwardToView() {
        location = new Div();
        location.setId(LOCATION_ID);
        param = new Div();
        param.setId(PARAMETER_ID);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        if (parameter != null) {
            switch (parameter) {
            case "location":
                event.forwardTo(
                        "com.vaadin.flow.uitest.ui.SetParameterForwardToView/locationTwo");
                break;
            case "locationRouteParameter":
                event.forwardTo(
                        "com.vaadin.flow.uitest.ui.SetParameterForwardToView",
                        "locationRouteParameterTwo");
                break;
            case "locationRouteParameterList":
                event.forwardTo(
                        "com.vaadin.flow.uitest.ui.SetParameterForwardToView",
                        List.of("locationRouteParameterListTwo"));
                break;
            case "locationQueryParams":
                event.forwardTo(
                        "com.vaadin.flow.uitest.ui.SetParameterForwardToView/locationQueryParamsTwo",
                        QueryParameters.empty());
                break;
            }
        }
        param.setText(parameter);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        location.setText(event.getLocation().getPath());
        add(location, param);
    }
}
