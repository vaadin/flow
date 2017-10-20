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
package com.vaadin.flow.uitest.ui.template;

import java.util.Locale;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.EventHandlerView", layout = ViewTestLayout.class)
@Tag("event-handler")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/EventHandler.html")
public class EventHandlerView extends PolymerTemplate<TemplateModel> {
    public EventHandlerView() {
        setId("template");
    }

    @EventHandler
    private void handleClick() {
        Element label = ElementFactory.createLabel("Event handler is invoked");
        label.setAttribute("id", "event-handler-result");
        getParent().get().getElement().appendChild(label);
    }

    @EventHandler
    private void sendData(@EventData("event.button") int button,
            @EventData("event.type") String type,
            @EventData("event.srcElement.tagName") String tag) {
        Element container = ElementFactory.createDiv();
        container.appendChild(ElementFactory
                .createDiv("Recieved event from the client with the data:"));
        container.appendChild(ElementFactory.createDiv("button: " + button));
        container.appendChild(ElementFactory.createDiv("type: " + type));
        container.appendChild(ElementFactory
                .createDiv("tag: " + tag.toLowerCase(Locale.ENGLISH)));
        container.setAttribute("id", "event-data");
        getParent().get().getElement().appendChild(container);
    }

    @EventHandler
    private void overriddenClick(@EventData("event.result") String result) {
        Element label = ElementFactory.createLabel(
                "Overridden server event was invoked with result: " + result);
        label.setAttribute("id", "overridden-event-handler-result");
        getParent().get().getElement().appendChild(label);
    }

    @ClientDelegate
    private void handleClientCall(String msg, boolean enabled) {
        Element div = ElementFactory.createDiv(
                "Call from client, message: " + msg + ", " + enabled);
        div.setAttribute("id", "client-call");
        getParent().get().getElement().appendChild(div);
    }
}
