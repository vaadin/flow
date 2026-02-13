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
package com.vaadin.tests.server.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Serializable test component using ComponentEffect and local signal.
 */
@Tag("div")
class SerializedLocalSignalComponent extends Component {
    int effectExecutionCounter = 0;
    ValueSignal<String> signal;
    Registration registration;

    SerializedLocalSignalComponent(ValueSignal<String> signal) {
        this.signal = signal;

        registration = ComponentEffect.effect(this, () -> {
            signal.get();
            effectExecutionCounter++;
        });

        getElement().bindText(signal);
        getElement().bindAttribute("attr", signal);
        getElement().bindProperty("prop", signal);
        getElement().bindEnabled(
                signal.map(value -> value != null && !value.isEmpty()));
        getElement().bindVisible(Signal.computed(() -> signal.get() != null));

        getElement().bindProperty("two-way-prop", signal.map(str -> str + "!!!",
                (str, value) -> value.replace("!!!", "")));

        // sync property from the client
        getElement().addPropertyChangeListener("two-way-prop",
                "two-way-prop-changed", e -> {
                });
    }

}
