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

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class SynchronizedPropertyUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(new Element("span")
                .setTextContent("Synchronized on 'change' event"));
        Element input = new Element("input").setAttribute("placeholder",
                "Enter text here");
        input.setSynchronizedProperties("value")
                .setSynchronizedPropertiesEvents("change");
        Element label = new Element("div");
        label.setTextContent("Server value: " + input.getProperty("value"));
        input.addEventListener("change", e -> {
            label.setTextContent("Server value: " + input.getProperty("value"));
        });
        getElement().appendChild(input, label);

        getElement().appendChild(new Element("span")
                .setTextContent("Synchronized on 'input' event"));
        Element input2 = new Element("input").setAttribute("placeholder",
                "Enter text here");
        input2.setSynchronizedProperties("value")
                .setSynchronizedPropertiesEvents("input");
        Element label2 = new Element("div");
        label2.setTextContent("Server value: " + input2.getProperty("value"));
        input2.addEventListener("input", e -> {
            label2.setTextContent(
                    "Server value: " + input2.getProperty("value"));
        });
        getElement().appendChild(input2, label2);
    }
}
