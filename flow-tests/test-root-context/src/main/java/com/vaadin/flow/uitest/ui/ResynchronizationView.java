package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Test for https://github.com/vaadin/flow/issues/7590
 */
@Route("com.vaadin.flow.uitest.ui.ResynchronizationView")
public class ResynchronizationView extends AbstractDivView {
    final static String ADD_BUTTON = "add";
    final static String ADDED_CLASS = "added";

    public ResynchronizationView() {
        add(createButton("Desync and add", ADD_BUTTON, e -> {
            final Span added = new Span("added");
            added.addClassName(ADDED_CLASS);
            add(added);
            // trigger a resynchronization request on the client
            getUI().get().getInternals().incrementServerId();
        }));
    }
}
