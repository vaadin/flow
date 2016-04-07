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
import com.vaadin.hummingbird.dom.ElementFactory;

public class SynchronizedPropertyView extends AbstractDivView {

    @Override
    protected void onShow() {
        getElement().appendChild(
                ElementFactory.createSpan("Synchronized on 'change' event"));
        Element input = ElementFactory.createInput().setAttribute("placeholder",
                "Enter text here");
        input.synchronizeProperty("value", "change");
        Element label = ElementFactory
                .createDiv("Server value: " + input.getProperty("value"));
        input.addEventListener("change", e -> {
            label.setTextContent("Server value: " + input.getProperty("value"));
        });
        getElement().appendChild(input, label);

        getElement().appendChild(
                ElementFactory.createSpan("Synchronized on 'input' event"));
        Element input2 = ElementFactory.createInput()
                .setAttribute("placeholder", "Enter text here");
        input2.synchronizeProperty("value", "input");
        Element label2 = ElementFactory
                .createDiv("Server value: " + input2.getProperty("value"));
        input2.addEventListener("input", e -> {
            label2.setTextContent(
                    "Server value: " + input2.getProperty("value"));
        });
        getElement().appendChild(input2, label2);
    }
}
