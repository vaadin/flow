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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class StateTreeUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        StateNode rootNode = getStateTree().getRootNode();
        Element bodyElement = Element.get(rootNode);

        Element button = new Element("input");
        button.setAttribute("type", "button");
        button.setAttribute("value", "Click me");

        button.addEventListener("click", () -> {
            Element greeting = new Element("div");
            greeting.setAttribute("class", "thankYou");
            greeting.getNode().getNamespace(ElementPropertiesNamespace.class)
                    .setProperty("textContent", "Thank you for clicking!");
            bodyElement.appendChild(greeting);
        });

        bodyElement.appendChild(button);
    }
}
