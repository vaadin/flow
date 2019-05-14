package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SetParameterForwardToView", layout = ViewTestLayout.class)
public class SetParameterForwardToView extends Div
        implements HasUrlParameter<String>, AfterNavigationObserver {

    static final String LOCATION_ID = "location";

    private final Div location;

    public SetParameterForwardToView() {
        location = new Div();
        location.setId(LOCATION_ID);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter != null && parameter.equals("one")) {
            event.forwardTo("com.vaadin.flow.uitest.ui.SetParameterForwardToView",
                    "two");
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        location.setText(event.getLocation().getPath());
        add(location);
    }
}
