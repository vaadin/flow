package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SetParameterForwardToView", layout = ViewTestLayout.class)
public class SetParameterForwardToView extends Div
        implements HasUrlParameter<String> {

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null) {
            event.forwardTo("com.vaadin.flow.uitest.ui.SetParameterForwardToView",
                    "auto");
        } else {
            setId(parameter);
        }
    }
}
