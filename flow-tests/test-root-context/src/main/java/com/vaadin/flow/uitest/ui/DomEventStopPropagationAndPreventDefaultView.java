/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
