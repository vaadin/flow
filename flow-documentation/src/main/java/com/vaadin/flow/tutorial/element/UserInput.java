/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.element;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.UI;

@CodeFor("element-api/tutorial-user-input.asciidoc")
public abstract class UserInput extends UI {

    void tutorialCode() {
        Element textInput = ElementFactory.createInput();
        textInput.setAttribute("placeholder", "Please enter your name");

        textInput.synchronizeProperty("value", "change");

        Element button = ElementFactory.createDiv();
        button.addEventListener("click", e -> {
            String responseText = "Hello " + textInput.getProperty("value");
            Element response = ElementFactory.createDiv(responseText);
            getElement().appendChild(response);
        });
    }
}
