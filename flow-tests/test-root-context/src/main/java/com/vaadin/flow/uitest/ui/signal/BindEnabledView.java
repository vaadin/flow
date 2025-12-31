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
package com.vaadin.flow.uitest.ui.signal;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.signals.ValueSignal;

/**
 * View for testing binding enabled state to a Signal.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.BindEnabledView", layout = ViewTestLayout.class)
public class BindEnabledView extends Div {

    private final ValueSignal<Boolean> signal1 = new ValueSignal<>(true);
    private final ValueSignal<Boolean> signal2 = new ValueSignal<>(true);

    private int counter;

    public BindEnabledView() {
        Div info = new Div();
        info.setId("click-info");

        Div targetEnabledInitially = new Div("Enabled initially");
        targetEnabledInitially.setId("target-enabled-initially");
        targetEnabledInitially.bindEnabled(signal1);
        targetEnabledInitially.addClickListener(event -> {
            info.setText("Clicked: " + (++counter));
        });

        Div parent = new Div();
        parent.setId("parent-enabled-initially");
        parent.bindEnabled(signal2);

        NativeButton toggleChildButton = new NativeButton(
                "Toggle enabled Signal value",
                e -> signal1.value(!signal1.peek()));
        toggleChildButton.setId("toggle-button-child");

        NativeButton toggleParentButton = new NativeButton(
                "Toggle parent enabled Signal value",
                e -> signal2.value(!signal2.peek()));
        toggleParentButton.setId("toggle-button-parent");

        parent.add(targetEnabledInitially);
        add(parent, toggleChildButton, toggleParentButton, info);
    }

}
