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
package com.vaadin.flow.test.cspstrict.push;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;

/**
 * A server push: the click starts a background task that updates the page after
 * the response to the click is sent, so the update can only reach the browser
 * over the push connection. With the update the view shows the transports that
 * the push requests of the browser have used, see
 * {@link PushTransportRecorder}. Push is enabled for the whole application, see
 * {@code Application}.
 */
abstract class AbstractPushView extends Div {

    private static final int DELAY_MS = 500;

    AbstractPushView() {
        Span pushed = new Span("waiting");
        pushed.setId("pushed");
        Span transports = new Span();
        transports.setId("transports");
        NativeButton start = new NativeButton("Start", click -> {
            UI ui = click.getSource().getUI().orElseThrow();
            beforePush(ui);
            CompletableFuture.runAsync(() -> ui.access(() -> {
                transports.setText(
                        PushTransportRecorder.getTransports(ui.getSession()));
                pushed.setText("Pushed under the policy");
            }), CompletableFuture.delayedExecutor(DELAY_MS,
                    TimeUnit.MILLISECONDS));
        });
        start.setId("start");
        add(start, pushed, transports);
    }

    /**
     * Called for the click that starts the background task, before the task is
     * started.
     *
     * @param ui
     *            the UI of the view
     */
    protected void beforePush(UI ui) {
        // Nothing by default
    }
}
