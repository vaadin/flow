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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.EventDataCapturesView", layout = ViewTestLayout.class)
public class EventDataCapturesView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div dataResult = new Div();
        dataResult.setId("dataResult");
        Div filterResult = new Div();
        filterResult.setId("filterResult");

        NativeButton dataButton = new NativeButton("Report event data");
        dataButton.setId("dataButton");
        dataButton.getElement().setAttribute("data-label", "captured label");
        dataButton.getElement()
                .addEventListener("click",
                        event -> dataResult.setText(
                                event.getEventData().get("label").asString()))
                .addEventData("label", "element.getAttribute($0)",
                        "data-label");

        // Two listeners using the same expression with different captures, to
        // show that their values and filters stay apart
        NativeButton filterButton = new NativeButton("Filtered by capture");
        filterButton.setId("filterButton");
        filterButton.getElement().addEventListener("click",
                event -> filterResult.setText("primary"))
                .setFilter("event.button === $0", 0);
        filterButton.getElement().addEventListener("click",
                event -> filterResult.setText("secondary"))
                .setFilter("event.button === $0", 2);

        add(dataButton, filterButton, dataResult, filterResult);
    }
}
