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
import com.vaadin.flow.html.Div;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;

/**
 * Test view for {@link TextField}.
 */
public class TextFieldView extends TestView {

    /**
     * Constructs a basic layout with a text field.
     */
    public TextFieldView() {
        initView();
    }

    private void initView() {
        Div message = new Div();
        TextField textField = new TextField();
        textField.addValueChangeListener(
                event -> message.setText(
                        String.format("Old value: '%s'. New value: '%s'.",
                                event.getOldValue(), event.getValue())));
        add(textField, message);

        Button button = new Button("Set/unset text field read-only");
        button.setId("read-only");
        button.addClickListener(
                event -> textField.setReadOnly(!textField.isReadOnly()));
        add(button);

        Button required = new Button("Set/unset field required property");
        required.setId("required");
        required.addClickListener(
                event -> textField.setRequiredIndicatorVisible(
                        !textField.isRequiredIndicatorVisible()));
        add(required);
    }
}
