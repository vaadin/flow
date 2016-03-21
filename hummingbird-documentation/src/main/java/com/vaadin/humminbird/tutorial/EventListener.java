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

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

/**
 * Tutorial code related to tutorial-event-listener.asciidoc.
 */
public abstract class EventListener extends UI {

    void tutorialCode() {
        Element helloButton = ElementFactory.createButton("Say hello");
        helloButton.addEventListener("click", e -> {
            Element response = ElementFactory.createDiv("Hello!");
            getElement().appendChild(response);
        });
        getElement().appendChild(helloButton);

        helloButton.addEventListener("click", e -> {
            JsonObject eventData = e.getEventData();
            boolean shiftKey = eventData.getBoolean("event.shiftKey");
            double width = eventData.getNumber("element.offsetWidth");

            String text = "Shift " + (shiftKey ? "down" : "up");
            text += " on button whose width is " + width + "px";

            Element response = ElementFactory.createDiv();
            response.setTextContent(text);
            getElement().appendChild(response);
        } , "event.shiftKey", "element.offsetWidth");
    }
}
