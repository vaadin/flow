/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

@Route("Select")
public class SelectView extends Div {

    public SelectView() {
        Div log = new Div();
        log.setId("log");

        Element select = new Element("select");
        for (int i = 1; i < 10; i++) {
            select.appendChild(
                    new Element("option").setAttribute("id", "id" + i)
                            .setAttribute("value", "value" + i)
                            .setText("Visible text " + i));
        }
        select.setAttribute("id", "input");
        select.addEventListener("change", e -> {
            log.setText("Value is '"
                    + e.getEventData().get("element.value").asText() + "'");
        }).synchronizeProperty("element.value");
        add(log);
        getElement().appendChild(select);
    }
}
