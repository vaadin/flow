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
package com.vaadin.flow.test.cspstrict.dom;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * The most ordinary thing an application does: a click listener on a component,
 * whose event carries data read from the DOM event, and a DOM event listener
 * with an event data expression on a plain element.
 */
@Route("click")
public class ClickView extends Div {

    public ClickView() {
        Span clicked = new Span();
        clicked.setId("clicked");
        NativeButton button = new NativeButton("Click me",
                event -> clicked.setText("clicked at x=" + event.getClientX()));
        button.setId("button");

        Span clientX = new Span();
        clientX.setId("client-x");
        Div area = new Div("Click the area");
        area.setId("area");
        area.getElement().addEventListener("click",
                event -> clientX.setText("x="
                        + event.getEventData().get("event.clientX").asInt()))
                .addEventData("event.clientX");

        add(button, clicked, area, clientX);
    }
}
