/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.util.function.Consumer;

import com.vaadin.annotations.DomEvent;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Hr;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Text;

public class CompositeView extends AbstractDivView {

    public static final String SERVER_INPUT_ID = "serverInput";
    public static final String SERVER_INPUT_BUTTON_ID = "serverInputButton";

    public static class NameField extends Composite {

        @DomEvent("change")
        public static class NameChangeEvent extends ComponentEvent {
            public NameChangeEvent(NameField source, boolean fromClient) {
                super(source, fromClient);
            }
        }

        private Input input = new Input();

        @Override
        protected Component initContent() {
            input.setPlaceholder("Enter your name");
            return input;
        }

        public void setName(String name) {
            input.setValue(name);
            fireEvent(new NameChangeEvent(this, false));
        }

        public String getName() {
            return input.getValue();
        }

        public void addNameChangeListener(
                Consumer<NameChangeEvent> nameChangeListener) {
            addListener(NameChangeEvent.class, nameChangeListener);
        }

        public void setId(String id) {
            input.setId(id);
        }
    }

    public CompositeView() {
        Div name = new Div("Name on server: ");
        name.setId(CompositeNestedView.NAME_ID);

        NameField nameField = new NameField();
        nameField.setId(CompositeNestedView.NAME_FIELD_ID);
        nameField.addNameChangeListener(e -> {
            name.setText("Name on server: " + nameField.getName());
            String text = "Name value changed to " + nameField.getName()
                    + " on the ";
            if (e.isFromClient()) {
                text += "client";
            } else {
                text += "server";
            }
            add(new Div(text));
        });
        add(name, nameField, new Hr());

        Input serverInput = new Input();
        serverInput.setId(SERVER_INPUT_ID);
        Button serverInputButton = new Button("Set", e -> {
            nameField.setName(serverInput.getValue());
            serverInput.setValue("");
        });
        serverInputButton.setId(SERVER_INPUT_BUTTON_ID);
        add(new Text("Enter a value to set the name on the server"),
                serverInput, serverInputButton);
    }
}
