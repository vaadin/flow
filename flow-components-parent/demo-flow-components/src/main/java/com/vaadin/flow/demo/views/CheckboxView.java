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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.checkbox.Checkbox;

/**
 * View for {@link Checkbox} demo.
 */
@Route(value = "vaadin-checkbox", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-checkbox.html")
@ComponentDemo(name = "Checkbox")
public class CheckboxView extends DemoView {

    @Override
    protected void initView() {
        addDefaultCheckbox();
        addDisabledCheckbox();
        addIndeterminateCheckbox();
        addValueChangeCheckbox();
        addAccessibleCheckbox();
    }

    private void addDefaultCheckbox() {
        // begin-source-example
        // source-example-heading: Default Checkbox
        Checkbox checkbox = new Checkbox();
        checkbox.setLabel("Default Checkbox");
        // end-source-example
        addCard("Default Checkbox", checkbox);
        checkbox.setId("default-checkbox");
    }

    private void addDisabledCheckbox() {
        // begin-source-example
        // source-example-heading: Disabled Checkbox
        Checkbox disabledCheckbox = new Checkbox("Disabled Checkbox");
        disabledCheckbox.setValue(true);
        disabledCheckbox.setDisabled(true);
        // end-source-example
        addCard("Disabled Checkbox", disabledCheckbox);
        disabledCheckbox.setId("disabled-checkbox");
    }

    private void addIndeterminateCheckbox() {
        // begin-source-example
        // source-example-heading: Indeterminate Checkbox
        Checkbox indeterminateCheckbox = new Checkbox("Indeterminate Checkbox");
        indeterminateCheckbox.setIndeterminate(true);
        // end-source-example

        Button button = new Button("Reset",
                event -> indeterminateCheckbox.setValue(null));
        button.setId("reset-indeterminate");

        addCard("Indeterminate Checkbox", indeterminateCheckbox, button);
        indeterminateCheckbox.setId("indeterminate-checkbox");
    }

    private void addValueChangeCheckbox() {
        // begin-source-example
        // source-example-heading: Checkbox with a ValueChangeListener
        Checkbox valueChangeCheckbox = new Checkbox(
                "Checkbox with a ValueChangeListener");
        Div message = new Div();
        valueChangeCheckbox.addValueChangeListener(event -> {
            message.setText(
                    String.format("Checkbox value changed from '%s' to '%s'",
                            event.getOldValue(), event.getValue()));
        });
        // end-source-example
        addCard("Checkbox with a ValueChangeListener", valueChangeCheckbox,
                message);
        valueChangeCheckbox.setId("value-change-checkbox");
        message.setId("value-change-checkbox-message");
    }

    private void addAccessibleCheckbox() {
        // begin-source-example
        // source-example-heading: Checkbox with Custom Accessible Label
        Checkbox accessibleCheckbox = new Checkbox("Accessible Checkbox");
        accessibleCheckbox.setAriaLabel("Click me");
        // end-source-example
        addCard("Checkbox with Custom Accessible Label", accessibleCheckbox);
        accessibleCheckbox.setId("accessible-checkbox");
    }
}
