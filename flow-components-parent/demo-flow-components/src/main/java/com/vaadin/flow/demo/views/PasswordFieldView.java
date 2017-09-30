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
import com.vaadin.ui.button.Button;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Label;
import com.vaadin.ui.layout.FlexLayout;
import com.vaadin.ui.layout.HorizontalLayout;
import com.vaadin.ui.passwordfield.PasswordField;
import com.vaadin.ui.textfield.GeneratedVaadinPasswordField;

/**
 * View for {@link GeneratedVaadinPasswordField} demo.
 */
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-text-field.html")
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-button.html")
@ComponentDemo(name = "Password Field", href = "vaadin-password-field")
public class PasswordFieldView extends DemoView {

    @Override
    void initView() {
        // begin-source-example
        // source-example-heading: Basic password field
        PasswordField passwordField = new PasswordField();
        passwordField.setLabel("Password field label");
        passwordField.setPlaceholder("Password");

        Label message = new Label(
                updateMessageText(passwordField.isPasswordVisible()));

        Button button = new Button("Toggle eye icon", event -> passwordField
                .setRevealButtonHidden(!passwordField.isRevealButtonHidden()));

        getElement().addPropertyChangeListener("passwordVisible",
                event -> message.setText(
                        updateMessageText(passwordField.isPasswordVisible())));
        // end-source-example

        button.setId("toggleButton");
        passwordField.setId("passwordField");
        message.setId("messageLabel");

        HorizontalLayout layout = new HorizontalLayout(passwordField, button);
        layout.setDefaultVerticalComponentAlignment(FlexLayout.Alignment.END);
        addCard("Basic password field", layout, message);
    }

    private String updateMessageText(boolean isPasswordVisible) {
        return "Password is " + (isPasswordVisible ? "visible" : "hidden");
    }
}
