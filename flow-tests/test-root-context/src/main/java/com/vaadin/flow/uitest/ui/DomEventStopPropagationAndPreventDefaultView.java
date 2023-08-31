/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomEventStopPropagationAndPreventDefaultView", layout = ViewTestLayout.class)
public class DomEventStopPropagationAndPreventDefaultView
        extends AbstractDivView {

    private final Element messages = new Element("div");

    public DomEventStopPropagationAndPreventDefaultView() {

        Element div = new Element("div");
        div.setAttribute("id", "btn");
        div.setText("btn");

        DomListenerRegistration reg = div.addEventListener("click", e -> {
            addMessage("event:btn");
        });

        reg.stopPropagation();

        // DomListenerRegistration.preventDefault()
        // This should not be received if the btn is clicked
        // as stopPropagation is called
        getElement().addEventListener("click", e -> {
            addMessage("event:div");
        });

        // Note: no good way to test this using selenium, test manually instead
        div.addEventListener("contextmenu", e -> {
            addMessage("event:contextmenu");
        }).preventDefault(); // now browser should not show default context menu

        getElement().appendChild(div);

        getElement().appendChild(messages);

    }

    private void addMessage(String message) {
        Element element = new Element("div");
        element.setText(message);
        messages.appendChild(element);
    }
}
