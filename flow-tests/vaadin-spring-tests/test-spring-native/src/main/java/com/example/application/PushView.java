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
package com.example.application;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("push")
public class PushView extends Div {

    public static final String START_ID = "start";
    public static final String STATUS_ID = "status";
    public static final String PUSHED = "Pushed from the server";

    public PushView() {
        Span status = new Span();
        status.setId(STATUS_ID);
        NativeButton start = new NativeButton("Update later", event -> {
            UI ui = UI.getCurrent();
            // Delayed so that the update cannot travel in the response to the
            // click: only a push connection brings it to the browser.
            CompletableFuture.runAsync(
                    () -> ui.access(() -> status.setText(PUSHED)),
                    CompletableFuture.delayedExecutor(500,
                            TimeUnit.MILLISECONDS));
        });
        start.setId(START_ID);
        add(start, status);
    }
}
