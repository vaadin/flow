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
package com.vaadin.flow.tutorial.element;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.tutorial.annotations.CodeFor;

import elemental.json.JsonObject;

@CodeFor("element-api/tutorial-event-listener.asciidoc")
public abstract class EventListener extends UI {

    void tutorialCode() {
        Element helloButton = ElementFactory.createButton("Say hello");
        helloButton.addEventListener("click", e -> {
            Element response = ElementFactory.createDiv("Hello!");
            getElement().appendChild(response);
        });
        getElement().appendChild(helloButton);

        //@formatter:off - custom line wrapping
        helloButton.addEventListener("click", this::handleClick, "event.shiftKey", "element.offsetWidth");
        //@formatter:on
    }

    private void handleClick(DomEvent event) {
        JsonObject eventData = event.getEventData();
        boolean shiftKey = eventData.getBoolean("event.shiftKey");
        double width = eventData.getNumber("element.offsetWidth");

        String text = "Shift " + (shiftKey ? "down" : "up");
        text += " on button whose width is " + width + "px";

        Element response = ElementFactory.createDiv(text);
        getElement().appendChild(response);
    }
}
