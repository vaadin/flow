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

import java.util.Set;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class BasicElementUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element button = new Element("input");
        button.setAttribute("type", "button");
        button.setAttribute("value", "Click me");

        button.addEventListener("click", () -> {
            Element greeting = new Element("div");
            greeting.setAttribute("class", "thankYou");
            greeting.setTextContent("Thank you for clicking!");
            greeting.addEventListener("click",
                    () -> greeting.removeFromParent());

            bodyElement.appendChild(greeting);
        });

        Element helloWorldElement = new Element("div");

        Set<String> spanClasses = helloWorldElement.getClassList();

        helloWorldElement.setProperty("id", "hello-world");
        helloWorldElement.setTextContent("Hello world");
        spanClasses.add("hello");
        helloWorldElement.addEventListener("click", () -> {
            helloWorldElement.setTextContent("Stop touching me!");
            spanClasses.clear();
        });
        Style s = helloWorldElement.getStyle();
        s.set("color", "red");
        s.set("fontWeight", "bold");

        bodyElement.appendChild(helloWorldElement, button);
    }

}
