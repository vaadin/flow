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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SynchronizedPropertyView", layout = ViewTestLayout.class)
public class SynchronizedPropertyView extends AbstractDivView {

    @Tag("input")
    public static class InputSync extends Component {
        public InputSync(Div label, String event) {
            getElement().setAttribute("placeholder", "Enter text here");
            label.setText("Server value on create: "
                    + getElement().getProperty("value"));
            getElement().addPropertyChangeListener("value", event, e -> {
            });
            getElement().addEventListener(event, e -> {
                label.setText(
                        "Server value: " + getElement().getProperty("value"));
            });
        }

        public void setValue(String value) {
            getElement().setProperty("value", value);
        }
    }

    @Override
    protected void onShow() {
        valueAsNumberShimForIE11();
        addSimpleSync();
        addSyncWithInitialValue();
        addSyncOnKeyup();
        addSyncMultipleProperties();
        addSyncWithCustomEventDebounce();
    }

    private void addSimpleSync() {
        add(new Text("Synchronized on 'change' event"));
        Div label = new Div();
        label.setId("syncOnChangeLabel");
        InputSync syncOnChange = new InputSync(label, "change");
        syncOnChange.setId("syncOnChange");
        add(syncOnChange);
        add(label);
    }

    private void addSyncWithInitialValue() {
        add(new Text("Synchronized on 'change' event with initial value"));
        final Div syncOnChangeInitialValueLabel = new Div();
        syncOnChangeInitialValueLabel.setId("syncOnChangeInitialValueLabel");
        Element syncOnChangeInitialValue = ElementFactory.createInput();
        syncOnChangeInitialValueLabel.setText("Server value on create: "
                + syncOnChangeInitialValue.getProperty("value"));
        syncOnChangeInitialValue.setAttribute("id", "syncOnChangeInitialValue");
        syncOnChangeInitialValue.addPropertyChangeListener("value", "change",
                event -> {
                });
        syncOnChangeInitialValue.addEventListener("change", e -> {
            syncOnChangeInitialValueLabel
                    .setText("Server value in change listener: "
                            + syncOnChangeInitialValue.getProperty("value"));
        });
        syncOnChangeInitialValue.setProperty("value", "initial");

        getElement().appendChild(syncOnChangeInitialValue);
        add(syncOnChangeInitialValueLabel);
    }

    private void addSyncOnKeyup() {
        Div label;
        add(new Text("Synchronized on 'keyup' event"));
        label = new Div();
        label.setId("syncOnKeyUpLabel");
        InputSync syncOnKeyUp = new InputSync(label, "keyup");
        syncOnKeyUp.setId("syncOnKeyUp");
        add(syncOnKeyUp);
        add(label);
    }

    private void addSyncMultipleProperties() {
        add(new Text(
                "Synchronize 'value' on 'input' event and 'valueAsNumber' on 'blur'"));
        Div valueLabel = new Div();
        valueLabel.setId("multiSyncValueLabel");
        Div valueAsNumberLabel = new Div();
        valueAsNumberLabel.setId("multiSyncValueAsNumberLabel");

        Element multiSync = ElementFactory.createInput("number");
        multiSync.setAttribute("id", "multiSyncValue");
        multiSync.addPropertyChangeListener("valueAsNumber", "blur", event -> {
        });
        multiSync.addPropertyChangeListener("value", "input", event -> {
        });

        multiSync.addEventListener("input", e -> {
            valueLabel
                    .setText("Server value: " + multiSync.getProperty("value"));
        });
        multiSync.addEventListener("blur", e -> {
            valueAsNumberLabel.setText("Server valueAsNumber: "
                    + multiSync.getProperty("valueAsNumber"));
        });

        getElement().appendChild(multiSync);
        add(valueLabel, valueAsNumberLabel);
    }

    private void addSyncWithCustomEventDebounce() {
        add(new Text(
                "Synchronize custom 'myProperty' on custom 'input' event"));
        Div valueLabel = new Div();
        valueLabel.setId("syncWithCustomEventDebounceLabel");

        Input input = new Input();
        input.setId("syncWithCustomEventDebounceInput");
        input.getElement().setProperty("myProperty", "");
        input.getElement()
                .addPropertyChangeListener("myProperty", "input", event -> {
                    valueLabel.setText("Server value: " + event.getValue());
                }).debounce(500);
        add(input, valueLabel);
    }

    /**
     * Fixes the broken behavior of valueAsNumber property on IE11, used in
     * {@link #addSyncMultipleProperties()}
     */
    private void valueAsNumberShimForIE11() {
        getPage().executeJs(
        //@formatter:off
        "var input = document.createElement('input');"
      + "input.setAttribute('type', 'number');"
      + "input.setAttribute('value', '123');"

        // This is true only on IE11, and is fixed below:
      + "if (input.value != input.valueAsNumber) {"
      + "    Object.defineProperty(Object.getPrototypeOf(input), 'valueAsNumber', {"
      + "        get: function () { return parseInt(this.value, 10); }"
      + "    });"
      + "}");
        //@formatter:on
    }

}
