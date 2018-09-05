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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomEventFilterView", layout = ViewTestLayout.class)
public class DomEventFilterView extends AbstractDivView {

    @Tag("input")
    public static class DebounceComponent extends Component {
        public Registration addInputListener(
                ComponentEventListener<InputEvent> listener,
                int debounceTimeout) {
            return getEventBus().addListener(InputEvent.class, listener,
                    domReg -> domReg.debounce(debounceTimeout));
        }
    }

    @DomEvent("input")
    public static class InputEvent extends ComponentEvent<DebounceComponent> {
        private String value;

        public InputEvent(DebounceComponent source, boolean fromClient,
                @EventData("element.value") String value) {
            super(source, fromClient);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final Element messages = new Element("div");

    public DomEventFilterView() {
        Element space = new Element("input");
        space.setAttribute("id", "space");

        space.addEventListener("keypress",
                e -> addMessage("Space listener triggered"))
                .setFilter("event.key == ' ' || event.key == 'Spacebar'");
        // The key is called 'Spacebar' on IE11

        Element debounce = new Element("input");
        debounce.setAttribute("id", "debounce");

        debounce.addEventListener("input",
                e -> addMessage("Trailing: "
                        + e.getEventData().getString("element.value")))
                .debounce(1000).addEventData("element.value");
        debounce.addEventListener("input",
                e -> addMessage("Leading: "
                        + e.getEventData().getString("element.value")))
                .debounce(1000, DebouncePhase.LEADING);
        debounce.addEventListener("input",
                e -> addMessage("Throttle: "
                        + e.getEventData().getString("element.value")))
                .throttle(1000);

        DebounceComponent component = new DebounceComponent();
        component.setId("debounce-component");
        component.addInputListener(
                e -> addMessage("Component: " + e.getValue()), 1000);

        messages.setAttribute("id", "messages");
        getElement().appendChild(space, debounce, component.getElement(),
                messages);
    }

    private void addMessage(String message) {
        Element element = new Element("div");
        element.setText(message);
        messages.appendChild(element);
    }
}
