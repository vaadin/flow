/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.Text;

@Route(value = "com.vaadin.flow.uitest.ui.SynchronizedPropertyView", layout = ViewTestLayout.class)
public class SynchronizedPropertyView extends AbstractDivView {

    @Tag("input")
    public static class InputSync extends Component {
        public InputSync(Div label, String event) {
            getElement().setAttribute("placeholder", "Enter text here");
            label.setText("Server value on create: "
                    + getElement().getProperty("value"));
            getElement().synchronizeProperty("value", event);
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
        addSimpleSync();
        addSyncWithInitialValue();
        addSyncOnKeyup();
        addSyncMultipleProperties();

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
        syncOnChangeInitialValue.synchronizeProperty("value", "change");
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
        multiSync.synchronizeProperty("valueAsNumber", "blur");
        multiSync.synchronizeProperty("value", "input");

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

}
