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
package com.vaadin.flow.tutorial.component;

import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.event.ComponentEvent;

@CodeFor("creating-components/tutorial-component-events.asciidoc")
public class ComponentEvents {

    @DomEvent("change")
    public class ChangeEvent extends ComponentEvent<TextField> {
        public ChangeEvent(TextField source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Tag("input")
    public class TextField extends Component {
        public Registration addChangeListener(
                ComponentEventListener<ChangeEvent> listener) {
            return addListener(ChangeEvent.class, listener);
        }

        public void setValue(String value) {
            getElement().setAttribute("value", value);
            fireEvent(new ChangeEvent(this, false));
        }

        // Other component methods omitted
    }

    @DomEvent("click")
    public class ClickEvent extends ComponentEvent<NativeButton> {
        private final int button;

        public ClickEvent(NativeButton source, boolean fromClient,
                @EventData("event.button") int button) {
            super(source, fromClient);
            this.button = button;
        }

        public int getButton() {
            return button;
        }
    }

    @SuppressWarnings("unused")
    private void useEvents() {
        TextField textField = new TextField();
        Registration registration = textField
                .addChangeListener(e -> System.out.println("Event fired"));

        // In some other part of the code
        registration.remove();
    }

}
