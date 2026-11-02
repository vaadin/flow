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
package com.vaadin.flow.test.dom;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;

@Route("event-data-captures")
public class EventDataCapturesView extends Div {

    @JsDefinition
    public interface EventJs extends Serializable {
        @JsExpression("return element.getAttribute($0);")
        DomEventData<String> attribute(String name);

        @JsExpression("return event.button === $0;")
        DomEventFilter isButton(int button);
    }

    private static final EventJs JS = JsSnippets.of(EventJs.class);

    public EventDataCapturesView() {
        Div dataResult = new Div();
        dataResult.setId("dataResult");
        Div filterResult = new Div();
        filterResult.setId("filterResult");

        DomEventData<String> label = JS.attribute("data-label");
        NativeButton dataButton = new NativeButton("Report event data");
        dataButton.setId("dataButton");
        dataButton.getElement().setAttribute("data-label", "captured label");
        dataButton.getElement()
                .addEventListener("click",
                        event -> dataResult.setText(event.getEventData(label)))
                .addEventData(label);

        // Two listeners using the same declared filter with different
        // captures, to show that their filters stay apart
        NativeButton filterButton = new NativeButton("Filtered by capture");
        filterButton.setId("filterButton");
        filterButton.getElement()
                .addEventListener("click",
                        event -> filterResult.setText("primary"))
                .setFilter(JS.isButton(0));
        filterButton.getElement()
                .addEventListener("click",
                        event -> filterResult.setText("secondary"))
                .setFilter(JS.isButton(2));

        add(dataButton, filterButton, dataResult, filterResult);
    }
}
