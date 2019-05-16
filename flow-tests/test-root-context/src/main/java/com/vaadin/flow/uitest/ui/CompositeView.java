/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.webcomponent.PaperSlider;

@Route(value = "com.vaadin.flow.uitest.ui.CompositeView", layout = ViewTestLayout.class)
public class CompositeView extends AbstractDivView {

    public static final String SERVER_INPUT_ID = "serverInput";
    public static final String SERVER_INPUT_BUTTON_ID = "serverInputButton";
    public static final String COMPOSITE_PAPER_SLIDER = "compositePaperSlider";

    public static class NameField extends Composite<Input> {

        @DomEvent("change")
        public static class NameChangeEvent extends ComponentEvent<NameField> {
            public NameChangeEvent(NameField source, boolean fromClient) {
                super(source, fromClient);
            }
        }

        private Input input = new Input();

        @Override
        protected Input initContent() {
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
                ComponentEventListener<NameChangeEvent> nameChangeListener) {
            addListener(NameChangeEvent.class, nameChangeListener);
        }

        @Override
        public void setId(String id) {
            input.setId(id);
        }
    }

    public static class PaperSliderComposite extends Composite<PaperSlider> {
    }

    public CompositeView() {
        Div name = new Div();
        name.setText("Name on server: ");
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
            Div changeMessage = new Div();
            changeMessage.setText(text);
            add(changeMessage);
        });
        add(name, nameField, new Hr());

        Input serverInput = new Input();
        serverInput.setId(SERVER_INPUT_ID);
        NativeButton serverInputButton = createButton("Set",
                SERVER_INPUT_BUTTON_ID, e -> {
            nameField.setName(serverInput.getValue());
            serverInput.clear();
        });
        add(new Text("Enter a value to set the name on the server"),
                serverInput, serverInputButton);

        add(new Hr());

        PaperSliderComposite paperSliderComposite = new PaperSliderComposite();
        paperSliderComposite.setId(COMPOSITE_PAPER_SLIDER);
        add(paperSliderComposite);
    }
}
