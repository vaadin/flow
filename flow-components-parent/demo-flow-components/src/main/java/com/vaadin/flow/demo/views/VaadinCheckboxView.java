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
package com.vaadin.flow.demo.views;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.H3;
import com.vaadin.ui.VaadinCheckbox;

/**
 * View for {@link VaadinCheckbox} demo.
 */
@ComponentDemo(name = "Vaadin Checkbox", href = "vaadin-checkbox")
public class VaadinCheckboxView extends DemoView {

    @Override
    void initView() {
        addDefaultCheckbox();
        addDisabledCheckbox();
        addIndeterminateCheckbox();
        addValueChangeCheckbox();
        addAccessibleCheckbox();
    }

    private void addDefaultCheckbox() {
        // begin-source-example
        // source-example-heading: Default Checkbox
        VaadinCheckbox checkbox = new VaadinCheckbox();
        checkbox.setLabelText("Default Checkbox");
        // end-source-example
        add(new H3("Default Checkbox"), checkbox);
        checkbox.setId("default-checkbox");
    }

    private void addDisabledCheckbox() {
        // begin-source-example
        // source-example-heading: Disabled Checkbox
        VaadinCheckbox disabledCheckbox = new VaadinCheckbox(
                "Disabled Checkbox").setValue(true).setDisabled(true);
        // end-source-example
        addCard(new H3("Disabled Checkbox"), disabledCheckbox);
        disabledCheckbox.setId("disabled-checkbox");
    }

    private void addIndeterminateCheckbox() {
        // begin-source-example
        // source-example-heading: Indeterminate Checkbox
        VaadinCheckbox indeterminateCheckbox = new VaadinCheckbox(
                "Indeterminate Checkbox").setIndeterminate(true);
        // end-source-example
        addCard(new H3("Indeterminate Checkbox"), indeterminateCheckbox);
        indeterminateCheckbox.setId("indeterminate-checkbox");
    }

    private void addValueChangeCheckbox() {
        // begin-source-example
        // source-example-heading: Checkbox with a ValueChangeListener
        VaadinCheckbox valueChangeCheckbox = new VaadinCheckbox(
                "Checkbox with a ValueChangeListener");
        Div message = new Div();
        valueChangeCheckbox.addValueChangeListener(event -> {
            message.setText(
                    String.format("Checkbox value changed from '%s' to '%s'",
                            event.getOldValue(), event.getValue()));
        });
        // end-source-example
        addCard(new H3("Checkbox with a ValueChangeListener"),
                valueChangeCheckbox, message);
        valueChangeCheckbox.setId("value-change-checkbox");
        message.setId("value-change-checkbox-message");
    }

    private void addAccessibleCheckbox() {
        // begin-source-example
        // source-example-heading: Checkbox with Custom Accessible Label
        VaadinCheckbox accessibleCheckbox =
                new VaadinCheckbox("Accessible Checkbox")
                    .setAriaLabel("Click me");
        // end-source-example
        addCard(new H3("Checkbox with Custom Accessible Label"),
                accessibleCheckbox);
        accessibleCheckbox.setId("accessible-checkbox");
    }
}
