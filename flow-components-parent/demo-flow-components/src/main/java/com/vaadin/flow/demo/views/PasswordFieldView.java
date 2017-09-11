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
import com.vaadin.generated.vaadin.text.field.GeneratedVaadinPasswordField;
import com.vaadin.ui.Button;
import com.vaadin.ui.PasswordField;

/**
 * View for {@link GeneratedVaadinPasswordField} demo.
 */
@ComponentDemo(name = "Password Field", href = "vaadin-password-field")
public class PasswordFieldView extends DemoView {
    @Override
    void initView() {
        Div message = new Div();

        // begin-source-example
        // source-example-heading: Basic password field
        PasswordField passwordField = new PasswordField();
        passwordField.setLabel("Password field label");
        passwordField.setPlaceholder("placeholder text");
        passwordField.addValueChangeListener(event -> message.setText(
                String.format("Password field value changed from '%s' to '%s'",
                        event.getOldValue(), event.getValue())));
        Button button = new Button("Toggle eye icon if password is hidden",
                event -> {
                    if (!passwordField.isPasswordVisible()) {
                        passwordField.setRevealButtonHidden(
                                !passwordField.isRevealButtonHidden());
                    }
                });
        // end-source-example

        passwordField.setId("password-field-with-value-change-listener");
        message.setId("password-field-value");
        button.setId("toggle-button");

        addCard("Basic password field", button, passwordField, message);
    }
}
