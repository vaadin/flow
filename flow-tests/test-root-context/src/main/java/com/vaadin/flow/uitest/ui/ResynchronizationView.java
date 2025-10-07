/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
