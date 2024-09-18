package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.ForwardingToParametersView")
public class ForwardingToParametersView extends Div
        implements BeforeEnterObserver {
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(ForwardTargetWithParametersView.class);
    }
}
