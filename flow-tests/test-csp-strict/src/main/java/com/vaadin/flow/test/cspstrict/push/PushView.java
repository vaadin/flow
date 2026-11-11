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
import com.vaadin.flow.router.Route;

/**
 * A server push: the click starts a background task that updates the page after
 * the response to the click is sent, so the update can only reach the browser
 * over the push connection. Push is enabled for the whole application, see
 * {@code Application}.
 */
@Route("push")
public class PushView extends Div {

    private static final int DELAY_MS = 500;

    public PushView() {
        Span pushed = new Span("waiting");
        pushed.setId("pushed");
        NativeButton start = new NativeButton("Start", click -> {
            UI ui = click.getSource().getUI().orElseThrow();
            CompletableFuture.runAsync(
                    () -> ui.access(
                            () -> pushed.setText("Pushed under the policy")),
                    CompletableFuture.delayedExecutor(DELAY_MS,
                            TimeUnit.MILLISECONDS));
        });
        start.setId("start");
        add(start, pushed);
    }
}
