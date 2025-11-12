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

    public static final String TARGET_DIV_ID = "target-div";
    public static final String RESULT_DIV_ID = "result-div";
    public static final String SIGNAL_VALUE_DIV_ID = "signal-value-div";
    public static final String SHOULD_THROW_TARGET_DIV_ID = "should-throw-target-div";
    public static final String LISTENER_COUNT_DIV_ID = "listener-count-div";
    public static final String TEST_PROPERTY_NAME = "testproperty";

    @Override
    protected void onShow() {
        AtomicInteger listenerCallCounter = new AtomicInteger();

        // Happy path for updating a property from client and
        // showing the value from property change listener as
        // well as from a signal effect
        Div target = createAndAddDiv(TARGET_DIV_ID);
        Div result = createAndAddDiv(RESULT_DIV_ID);
        Div signalValue = createAndAddDiv(SIGNAL_VALUE_DIV_ID);
        Div listenerCountDiv = createAndAddDiv(LISTENER_COUNT_DIV_ID);

        Signal<String> signal = new ValueSignal<>("foo");
        ComponentEffect.effect(this, () -> {
            signalValue.setText("Signal value: " + signal.value());
        });
        target.getElement().bindProperty(TEST_PROPERTY_NAME, signal);

        target.getElement().addPropertyChangeListener(TEST_PROPERTY_NAME,
                "change", event -> {
                    String newValue = (String) event.getValue();
                    result.setText(
                            TEST_PROPERTY_NAME + " changed to: " + newValue);
                    listenerCountDiv.setText(String
                            .valueOf(listenerCallCounter.incrementAndGet()));
                });

        // Attempt to update a property value from client to a computed signal
        // should throw an exception
        Div shouldThrowTarget = createAndAddDiv(SHOULD_THROW_TARGET_DIV_ID);
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.value());
        shouldThrowTarget.getElement().bindProperty(TEST_PROPERTY_NAME,
                computedSignal);
        shouldThrowTarget.getElement().addPropertyChangeListener(
                TEST_PROPERTY_NAME, "change", event -> {
                    // NOP; listener is needed to synchronize the property
                });
    }

    private Div createAndAddDiv(String id) {
        Div div = new Div();
        div.setText(id);
        div.setId(id);
        add(div);
        return div;
    }

}
