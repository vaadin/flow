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
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

/**
 * Test view for testing Signal binding with
 * {@link com.vaadin.flow.component.Component#bindVisible(Signal)}.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.BindVisibleView", layout = ViewTestLayout.class)
public class BindVisibleView extends Div {

    private final ValueSignal<Boolean> signal1 = new ValueSignal<>(true);
    private final ValueSignal<Boolean> signal2 = new ValueSignal<>(false);

    public BindVisibleView() {
        Div targetVisibleInitially = new Div("Visible initially");
        targetVisibleInitially.setId("target-visible-initially");
        targetVisibleInitially.bindVisible(signal1);

        Div targetHiddenInitially = new Div("Hidden initially");
        targetHiddenInitially.setId("target-hidden-initially");
        targetHiddenInitially.bindVisible(signal2);

        NativeButton toggleButton1 = new NativeButton(
                "Toggle visible Signal value",
                e -> signal1.value(!signal1.peek()));
        toggleButton1.setId("toggle-button-1");

        NativeButton toggleButton2 = new NativeButton(
                "Toggle visible Signal value",
                e -> signal2.value(!signal2.peek()));
        toggleButton2.setId("toggle-button-2");

        add(targetVisibleInitially, targetHiddenInitially, toggleButton1,
                toggleButton2);
    }
}
