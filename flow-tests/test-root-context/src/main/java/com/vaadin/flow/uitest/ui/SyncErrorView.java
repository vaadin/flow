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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * View for testing sync error behavior. This view provides buttons to enable
 * desync simulation and trigger actions that will cause sync errors.
 */
@Route("com.vaadin.flow.uitest.ui.SyncErrorView")
public class SyncErrorView extends Div {

    public SyncErrorView() {
        NativeButton enableDesyncButton = new NativeButton(
                "Enable Desync Simulation", event -> {
                    SimulateDesyncUidlRequestHandler
                            .enableDesync(UI.getCurrent().getSession());
                    add(new Span("Desync simulation enabled"));
                });
        enableDesyncButton.setId("enable-desync");

        NativeButton triggerActionButton = new NativeButton("Trigger Action",
                event -> {
                    // This action will fail if desync is enabled because
                    // MessageIdSyncException will be thrown
                    add(new Span("Action completed"));
                });
        triggerActionButton.setId("trigger-action");

        NativeButton disableDesyncButton = new NativeButton(
                "Disable Desync Simulation", event -> {
                    SimulateDesyncUidlRequestHandler
                            .disableDesync(UI.getCurrent().getSession());
                    add(new Span("Desync simulation disabled"));
                });
        disableDesyncButton.setId("disable-desync");

        add(enableDesyncButton, triggerActionButton, disableDesyncButton);
    }
}
