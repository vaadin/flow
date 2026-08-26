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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetSignalAction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@code DomEventTrigger} on an {@link Input}'s {@code input} event to
 * a {@link SetSignalAction} that pushes the field's current {@code value}
 * property into a {@link ValueSignal}. A {@link Signal#effect} mirrors the
 * signal value into a status {@link Div}, so the IT can assert that typing into
 * the field propagates client → signal → effect → DOM.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerSetSignalView", layout = ViewTestLayout.class)
public class TriggerSetSignalView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input field = new Input();
        field.setId("source");
        Div status = new Div();
        status.setId("status");
        add(field, status);

        ValueSignal<String> signal = new ValueSignal<>("");
        Signal.effect(this, () -> status.setText(signal.get()));

        new DomEventTrigger(field, "input")
                .triggers(new SetSignalAction<>(signal, String.class,
                        new PropertyInput<>(field, "value", String.class)));
    }
}
