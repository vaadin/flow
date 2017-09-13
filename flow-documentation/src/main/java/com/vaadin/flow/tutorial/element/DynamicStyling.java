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

@CodeFor("element-api/tutorial-dynamic-styling.asciidoc")
public class DynamicStyling {

    Element button = ElementFactory.createDiv();

    void tutorialCode() {

        button.setText("Change to blue");
        button.addEventListener("click",
                e -> button.getClassList().add("blue"));

        //@formatter:off - custom line wrapping

        Element input = ElementFactory.createInput();
        button.setText("Change to the entered value");
        button.addEventListener("click",
                e -> button.getStyle().set("background", input.getProperty("value")));

        //@formatter:on
    }
}
