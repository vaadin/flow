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

import java.util.function.BiConsumer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;

import elemental.json.Json;
import elemental.json.JsonObject;

@Route("com.vaadin.flow.uitest.ui.HistoryView")
public class HistoryView extends AbstractDivView {

    private final Element stateJsonInput = createSynchronizedInput("state");
    private final Element locationInput = createSynchronizedInput("location");

    public HistoryView() {
        History history = getPage().getHistory();

        addRow(Element.createText("State to set (JSON) "), stateJsonInput);
        addRow(Element.createText("Location to set "), locationInput);

        addRow(createStateButton("pushState", history::pushState),
                createStateButton("replaceState", history::replaceState));

        addRow(createActionButton("back", history::back),
                createActionButton("forward", history::forward));

        addRow(createActionButton("clear", this::clear));

        history.setHistoryStateChangeHandler(e -> {
            addStatus("New location: " + e.getLocation().getPath());

            e.getState().ifPresent(
                    state -> addStatus("New state: " + state.toJson()));
        });
    }

    private void clear() {
        while (true) {
            Element lastChild = getElement()
                    .getChild(getElement().getChildCount() - 1);
            if (lastChild.getClassList().contains("status")) {
                lastChild.removeFromParent();
            } else {
                return;
            }
        }
    }

    private Element createActionButton(String text, Command command) {
        return createButton(text, e -> command.execute());
    }

    private Element createStateButton(String text,
            BiConsumer<JsonObject, String> stateUpdater) {
        return createButton(text, e -> {
            String stateJsonString = stateJsonInput.getProperty("value", "");
            JsonObject stateJson;
            if (stateJsonString.isEmpty()) {
                stateJson = null;
            } else {
                stateJson = Json.parse(stateJsonString);
            }

            String location = locationInput.getProperty("value", "");

            stateJsonInput.setProperty("value", "");
            locationInput.setProperty("value", "");

            stateUpdater.accept(stateJson, location);
        });
    }

    private Element addRow(Element... elements) {
        Element row = ElementFactory.createDiv().appendChild(elements);
        getElement().appendChild(row);
        return row;
    }

    private void addStatus(String text) {
        Element statusRow = addRow(Element.createText(text));
        statusRow.getClassList().add("status");
    }

    private static Element createButton(String id,
            ComponentEventListener<ClickEvent<NativeButton>> listener) {
        return createButton(id, id, listener).getElement();
    }

    private static Element createSynchronizedInput(String id) {
        return ElementFactory.createInput().setAttribute("id", id)
                .synchronizeProperty("value", "change");
    }

}
