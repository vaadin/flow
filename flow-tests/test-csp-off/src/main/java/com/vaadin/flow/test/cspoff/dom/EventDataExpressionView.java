/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.cspoff.dom;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Adds event data to an element listener as JavaScript expressions that are not
 * property paths, with {@code addEventData(String)} and
 * {@code addEventDataElement(String)}, so the browser compiles them. Property
 * paths and declared event data are tested in the modules that run with the
 * strict mode.
 */
@Route("event-data-expression")
public class EventDataExpressionView extends Div {

    public static final String VALUE_EXPRESSION = "event.target.id + '-clicked'";
    public static final String ELEMENT_EXPRESSION = "event.composedPath()[0]";

    public static final String BUTTON_ID = "button";
    public static final String VALUE_ID = "value";
    public static final String ELEMENT_ID = "element";

    @SuppressWarnings("deprecation")
    public EventDataExpressionView() {
        NativeButton button = new NativeButton("Click");
        button.setId(BUTTON_ID);
        Div container = new Div(button);

        Div value = new Div();
        value.setId(VALUE_ID);
        Div element = new Div();
        element.setId(ELEMENT_ID);

        // The listener is on the container, so the event data has to find
        // the button that was clicked inside it
        container.getElement().addEventListener("click", event -> {
            value.setText(
                    event.getEventData().get(VALUE_EXPRESSION).asString());
            element.setText(event.getEventDataElement(ELEMENT_EXPRESSION)
                    .map(clicked -> clicked.getAttribute("id"))
                    .orElse("no element"));
        }).addEventData(VALUE_EXPRESSION)
                .addEventDataElement(ELEMENT_EXPRESSION);

        add(container, value, element);
    }
}
