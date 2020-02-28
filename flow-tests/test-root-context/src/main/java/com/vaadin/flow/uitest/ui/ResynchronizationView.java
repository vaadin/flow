package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * Test for https://github.com/vaadin/flow/issues/7590
 */
@Route("com.vaadin.flow.uitest.ui.ResynchronizationView")
public class ResynchronizationView extends AbstractDivView {
    final static String ID = "ResynchronizationView";

    final static String ADD_BUTTON = "add";
    final static String CALL_BUTTON = "call";

    final static String ADDED_CLASS = "added";
    final static String REJECTED_CLASS = "rejected";

    public ResynchronizationView() {
        setId(ID);

        add(createButton("Desync and add", ADD_BUTTON, e -> {
            final Span added = new Span("added");
            added.addClassName(ADDED_CLASS);
            add(added);
            triggerResync();
        }));

        add(createButton("Desync and call function", CALL_BUTTON, e -> {
            // add a span to <body> for the test (not to view, since the DOM
            // will be rebuilt on resync)
            final String js = String.format(
                    "document.getElementById(\"%s\").$server.clientCallable()"
                            + ".then(_ => {})"
                            + ".catch(_ => { document.body.innerHTML += '<span class=\"%s\">rejected</span>';});",
                    ID, REJECTED_CLASS);
            getUI().get().getPage().executeJs(js);
        }));
    }

    @ClientCallable
    public int clientCallable() {
        triggerResync();
        return 0;
    }

    private void triggerResync() {
        // trigger a resynchronization request on the client
        getUI().get().getInternals().incrementServerId();

    }
}
