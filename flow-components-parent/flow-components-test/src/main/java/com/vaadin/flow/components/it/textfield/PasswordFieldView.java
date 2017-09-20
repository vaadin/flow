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
package com.vaadin.flow.components.it.textfield;

import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.passwordfield.PasswordField;

/**
 * Test view for {@link PasswordField}.
 */
public class PasswordFieldView extends TestView {

    /**
     * Constructs a basic layout with a text field.
     */
    public PasswordFieldView() {
        Div message = new Div();
        PasswordField passwordField = new PasswordField();
        passwordField.addValueChangeListener(
                event -> message.setText(
                        String.format("Old value: '%s'. New value: '%s'.",
                                event.getOldValue(), event.getValue())));
        add(passwordField, message);

        Button button = new Button("Set/unset text field read-only");
        button.setId("read-only");
        button.addClickListener(
                event -> passwordField.setReadOnly(!passwordField.isReadOnly()));
        add(button);

        Button required = new Button("Set/unset field required property");
        required.setId("required");
        required.addClickListener(
                event -> passwordField.setRequiredIndicatorVisible(
                        !passwordField.isRequiredIndicatorVisible()));
        add(required);
    }
}
