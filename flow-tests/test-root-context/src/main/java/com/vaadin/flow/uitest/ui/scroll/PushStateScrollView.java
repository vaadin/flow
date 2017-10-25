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
package com.vaadin.flow.uitest.ui.scroll;

import java.util.function.BiConsumer;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.router.Route;
import com.vaadin.ui.History;
import com.vaadin.ui.UI;

import elemental.json.JsonValue;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.PushStateScrollView", layout = ViewTestLayout.class)
public class PushStateScrollView extends AbstractDivView {
    public PushStateScrollView() {
        Element filler = ElementFactory.createDiv(
                "Pushing or replacing history state should not affect the scroll position. Scroll down for buttons to click.");
        filler.getStyle().set("height", "150vh");

        History history = UI.getCurrent().getPage().getHistory();

        getElement().appendChild(filler,
                createButton("push", history::pushState),
                createButton("replace", history::replaceState));
    }

    private static Element createButton(String name,
            BiConsumer<JsonValue, String> action) {
        String location = PushStateScrollView.class.getName() + "/" + name;

        Element button = ElementFactory.createButton(name);

        button.setAttribute("id", name);
        button.addEventListener("click", e -> action.accept(null, location));

        return button;
    }
}
