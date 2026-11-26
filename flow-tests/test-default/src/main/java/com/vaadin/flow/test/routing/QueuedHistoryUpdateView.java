/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.routing;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Queues several history updates in one response, mixing updates that call back
 * to the server with updates that do not.
 */
@Route(QueuedHistoryUpdateView.ROUTE)
public class QueuedHistoryUpdateView extends Div
        implements BeforeEnterObserver {

    static final String ROUTE = "queued-history-update";
    static final String UPDATE_BUTTON_ID = "update";
    static final String LEAVE_BUTTON_ID = "leave";
    static final String CLIENT_ROUTE = "react";
    static final String ENTERED_ID = "entered";
    static final int ROUNDS = 10;

    private final List<String> enteredLocations = new ArrayList<>();
    private final Div entered = new Div();

    public QueuedHistoryUpdateView() {
        NativeButton update = new NativeButton("Update history", e -> {
            History history = getUI().orElseThrow().getPage().getHistory();
            for (int round = 1; round <= ROUNDS; round++) {
                history.replaceState(null, ROUTE + "?callback=" + round, true);
                history.replaceState(null, ROUTE + "?first=" + round);
                history.replaceState(null, ROUTE + "?second=" + round);
            }
        });
        update.setId(UPDATE_BUTTON_ID);

        NativeButton leave = new NativeButton(
                "Leave to client route and update history", e -> {
                    History history = getUI().orElseThrow().getPage()
                            .getHistory();
                    history.pushState(null, CLIENT_ROUTE, true);
                    history.replaceState(null, CLIENT_ROUTE + "?updated=1");
                    history.replaceState(null, CLIENT_ROUTE + "?updated=2");
                    history.replaceState(null, CLIENT_ROUTE + "?updated=3");
                });
        leave.setId(LEAVE_BUTTON_ID);

        entered.setId(ENTERED_ID);
        add(update, leave, entered);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        enteredLocations.add(event.getLocation().getPathWithQueryParameters());
        entered.setText(String.join(",", enteredLocations));
    }
}
