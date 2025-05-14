/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomEventStopPropagationAndPreventDefaultView", layout = ViewTestLayout.class)
public class DomEventStopPropagationAndPreventDefaultView
        extends AbstractDivView {

    @Tag("div")
    public static class DivComponent extends Component {

        public DivComponent() {
            getElement().setText("component");
            setId("component");
        }

        public Registration addClickListener(
                ComponentEventListener<ClickEvent> listener) {
            return getEventBus().addListener(ClickEvent.class, listener);
        }
    }

    @DomEvent(value = "click", stopPropagation = true, preventDefault = true)
    public static class ClickEvent extends ComponentEvent<DivComponent> {

        public ClickEvent(DivComponent source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    private final Element messages = new Element("div");

    public DomEventStopPropagationAndPreventDefaultView() {
        messages.setAttribute("id", "messages");

        Element div = new Element("div");
        div.setAttribute("id", "btn");
        div.setText("btn");

        DomListenerRegistration reg = div.addEventListener("click", e -> {
            addMessage("event:btn");
        });

        reg.stopPropagation();

        // DomListenerRegistration.preventDefault()
        // This should not be received if the btn is clicked
        // as stopPropagation is called
        getElement().addEventListener("click", e -> {
            addMessage("event:div");
        });

        getElement().addEventListener("contextmenu", e -> {
            addMessage("eventcontextmenu:div");
        });

        // Note: no good way to test this using selenium, test manually instead
        div.addEventListener("contextmenu", e -> {
            addMessage("event:contextmenu");
        }).preventDefault(); // now browser should not show default context menu

        getElement().appendChild(div);

        DivComponent divComponent = new DivComponent();
        divComponent.addClickListener(e -> {
            addMessage("event:component");
        });

        add(divComponent);

        getElement().appendChild(messages);

    }

    private void addMessage(String message) {
        Element element = new Element("div");
        element.setText(message);
        messages.appendChild(element);
    }
}
