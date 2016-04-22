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
package com.vaadin.humminbird.tutorial;

import java.util.function.Consumer;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventData;
import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;

@CodeFor("tutorial-component-events.asciidoc")
public class ComponentEvents {

    @DomEvent("change")
    public class ChangeEvent extends ComponentEvent<TextField> {
        public ChangeEvent(TextField source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Tag("input")
    public class TextField extends Component {
        public EventRegistrationHandle addChangeListener(
                Consumer<ChangeEvent> listener) {
            return addListener(ChangeEvent.class, listener);
        }

        public void setValue(String value) {
            getElement().setAttribute("value", value);
            fireEvent(new ChangeEvent(this, false));
        }

        // Other component methods omitted
    }

    @DomEvent("click")
    public class ClickEvent extends ComponentEvent<Button> {
        private final int button;

        public ClickEvent(Button source, boolean fromClient,
                @EventData("event.button") int button) {
            super(source, fromClient);
            this.button = button;
        }

        public int getButton() {
            return button;
        }
    }

    private void useEvents() {
        TextField textField = new TextField();
        EventRegistrationHandle registration = textField
                .addChangeListener(e -> System.out.println("Even fired"));

        // Later
        registration.remove();
    }

}
