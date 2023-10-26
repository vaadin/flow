package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ComponentErrorView", layout = ViewTestLayout.class)
public class ComponentErrorView extends Div {
    protected NativeButton throwException = new NativeButton("Throw");

    public ComponentErrorView() {
        throwException.addClickListener(event -> {
            throw new IllegalArgumentException("No clicking");
        });
        throwException.setId("throw");
        add(throwException);

        UI.getCurrent().getSession().setErrorHandler(error -> {
            Span componentPresent = new Span(
                    "" + error.getComponent().isPresent());
            componentPresent.setId("present");
            add(componentPresent);
            error.getComponent().ifPresent(component -> {
                Span componentName = new Span(component.getClass().getName());
                componentName.setId("name");
                add(componentName);
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        UI.getCurrent().getSession().setErrorHandler(new DefaultErrorHandler());
    }
}
