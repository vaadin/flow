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
package com.vaadin.flow.uitest.ui;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

@Route(value = "com.vaadin.flow.uitest.ui.ElementPropertySignalBindingView", layout = ViewTestLayout.class)
public class ElementPropertySignalBindingView extends AbstractDivView {

    @Override
    protected void onShow() {
        AtomicInteger listenerCounter = new AtomicInteger();

        Div target = new Div();
        target.setId("target-div");
        add(target);

        Div result = new Div();
        result.setId("result-div");
        add(result);

        Div signalValue = new Div();
        signalValue.setId("signal-value-div");
        add(signalValue);

        Div listenerCountDiv = new Div();
        listenerCountDiv.setId("listener-count-div");
        add(listenerCountDiv);

        Signal<String> signal = new ValueSignal<>("foo");
        ComponentEffect.effect(this, () -> {
            signalValue.setText("Signal value: " + signal.value());
        });
        target.getElement().bindProperty("testproperty", signal);

        target.getElement().addPropertyChangeListener("testproperty", "change",
                event -> {
                    String newValue = (String) event.getValue();
                    result.setText("testproperty changed to: " + newValue);
                    listenerCountDiv.setText(
                            String.valueOf(listenerCounter.incrementAndGet()));
                });
    }

}
