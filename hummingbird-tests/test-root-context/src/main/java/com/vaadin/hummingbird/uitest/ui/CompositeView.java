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

import java.util.EventObject;
import java.util.function.Consumer;

import com.vaadin.hummingbird.uitest.component.Button;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.hummingbird.uitest.component.Hr;
import com.vaadin.hummingbird.uitest.component.Input;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Text;

public class CompositeView extends AbstractDivView {

    public static final String SERVER_INPUT_ID = "serverInput";
    public static final String SERVER_INPUT_BUTTON_ID = "serverInputButton";

    public static class NameField extends Composite {

        public static class NameChangeEvent extends EventObject {

            private boolean clientOriginated;

            public NameChangeEvent(NameField source, boolean clientOriginated) {
                super(source);
                this.clientOriginated = clientOriginated;
            }

            public boolean isClientOriginated() {
                return clientOriginated;
            }

            @Override
            public NameField getSource() {
                return (NameField) super.getSource();
            }
        }

        private Input input = new Input();;
        private Consumer<NameChangeEvent> nameChangeListener = null;

        @Override
        protected Component initContent() {
            input.setPlaceholder("Enter your name");
            input.getElement().addEventListener("change", e -> {
                fireNameChangeEvent(true);
            });
            return input;
        }

        private void fireNameChangeEvent(boolean clientOriginated) {
            if (nameChangeListener != null) {
                nameChangeListener
                        .accept(new NameChangeEvent(this, clientOriginated));
            }

        }

        public void setName(String name) {
            input.setValue(name);
            fireNameChangeEvent(false);
        }

        public String getName() {
            return input.getValue();
        }

        public void setNameChangeListener(
                Consumer<NameChangeEvent> nameChangeListener) {
            this.nameChangeListener = nameChangeListener;
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
        nameField.setNameChangeListener(e -> {
            name.setText("Name on server: " + nameField.getName());
            String text = "Name value changed to " + nameField.getName()
                    + " on the ";
            if (e.isClientOriginated()) {
                text += "client";
            } else {
                text += "server";
            }
            addComponents(new Div(text));
        });
        addComponents(name, nameField, new Hr());

        Input serverInput = new Input();
        serverInput.setId(SERVER_INPUT_ID);
        Button serverInputButton = new Button("Set");
        serverInputButton.setId(SERVER_INPUT_BUTTON_ID);
        serverInputButton.getElement().addEventListener("click", e -> {
            nameField.setName(serverInput.getValue());
            serverInput.setValue("");
        });
        addComponents(new Text("Enter a value to set the name on the server"),
                serverInput, serverInputButton);
    }
}
