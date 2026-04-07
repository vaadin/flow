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

import java.util.Locale;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * View for testing binding value state to a Signal.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.BindValueView", layout = ViewTestLayout.class)
public class BindValueView extends Div {

    private final ValueSignal<String> signal = new ValueSignal<>("");

    private int counter;

    public BindValueView() {
        Div valueInfoDiv = new Div();
        valueInfoDiv.setId("value-info");

        Div counterDiv = new Div();
        counterDiv.setId("counter");

        Div signalValueInfoDiv = new Div(
                signal.map(value -> "Signal: " + value));
        signalValueInfoDiv.setId("signal-value-info");

        TestInput target = new TestInput();
        target.setId("target");
        target.bindValue(signal, signal::set);
        Registration targetListener = target.addValueChangeListener(event -> {
            valueInfoDiv.setText("Value: " + event.getValue());
            counter++;
            counterDiv.setText("ValueChange #" + counter);
        });

        TestInput target2 = new TestInput();
        target2.setId("target2");
        target2.addValueChangeListener(event -> {
            valueInfoDiv.setText("Value: " + event.getValue());
            counter++;
            counterDiv.setText("ValueChange #" + counter);
        });

        NativeButton changeInputValueButton = new NativeButton(
                "setValue(\"foo\")", e -> {
                    target.setValue("foo");
                });
        changeInputValueButton.setId("change-value-button");

        NativeButton changeSignalValueButton = new NativeButton(
                "signal.set(\"bar\")", e -> signal.set("bar"));
        changeSignalValueButton.setId("change-signal-value-button");

        NativeButton changeValueInternallyButton = new NativeButton(
                "setModelValue(\"bar\")",
                e -> target.setModelValue("bar", false));
        changeValueInternallyButton.setId("internal-change-value-button");

        NativeButton bindUppercaseInputValueButton = new NativeButton(
                "bindValue(signal, transformToUppercase)", e -> {
                    target.setEnabled(false);
                    // unregister other input's listener
                    targetListener.remove();
                    target2.bindValue(signal, value -> signal
                            .set(value.toUpperCase(Locale.ENGLISH)));
                });
        bindUppercaseInputValueButton.setId("bind-uppercase-value-button");

        NativeButton changeUppercaseInputValueButton = new NativeButton(
                "setValue(\"foo\")", e -> {
                    target2.setValue("foo");
                });
        changeUppercaseInputValueButton.setId("change-uppercase-value-button");

        add(target, changeInputValueButton, changeSignalValueButton,
                changeValueInternallyButton, target2,
                bindUppercaseInputValueButton, changeUppercaseInputValueButton,
                valueInfoDiv, signalValueInfoDiv, counterDiv);
    }

    private static class TestInput extends Input {
        // Make setModelValue public for testing purposes
        @Override
        public void setModelValue(String value, boolean fromClient) {
            super.setModelValue(value, fromClient);
        }
    }
}
