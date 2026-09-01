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
package com.vaadin.flow.uitest.ui;

import java.time.Duration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.TriggerAfterView", layout = ViewTestLayout.class)
public class TriggerAfterView extends AbstractDivView {

    static final String WAITING = "waiting";
    static final String FIRED = "fired";

    public TriggerAfterView() {
        Div status = new Div();
        status.setId("status");
        status.setText(WAITING);

        // Run a server-side task ~500 ms after the request that arms it,
        // triggered by a client timer round trip. No push enabled.
        add(createButton("Trigger after", "triggerAfter",
                e -> UI.getCurrent().triggerAfter(Duration.ofMillis(500),
                        () -> status.setText(FIRED))));

        add(status);
    }
}
