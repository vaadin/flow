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

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

@CodeFor("tutorial-properties-attributes.asciidoc")
public class PropertiesAttributes {

    @SuppressWarnings("unused")
    void tutorialCode() {
        Element nameField = ElementFactory.createInput();
        nameField.setAttribute("id", "nameField");
        nameField.setAttribute("placeholder", "John Doe");
        nameField.setAttribute("autofocus", "");

        // "John Doe"
        String placeholder = nameField.getAttribute("placeholder");

        // true
        nameField.hasAttribute("autofocus");

        nameField.removeAttribute("autofocus");

        // ["id", "placeholder"]
        nameField.getAttributeNames().toArray();

        Element element = ElementFactory.createInput();
        element.setProperty("value", "42.2");

        // true, since any non-empty string is true in JavaScript
        boolean helloBoolean = element.getProperty("value", true);

        // 42, string is parsed to a JS number and truncated to an int
        int helloInt = element.getProperty("value", 0);

        element.getClassList().add("error");
        element.getClassList().add("critical");
        element.getClassList().remove("primary");

        element.getProperty("className"); // will return "error critical".

        element.getStyle().set("color", "red");
        // Note the camelCase, not dash - separated
        element.getStyle().set("fontWeight", "bold");

        // Note the camelCase, not dash - separated
        element.getStyle().remove("backgroundColor");

        element.getStyle().has("cursor");
    }

    public void moreTutorialCode() {
        //@formatter:off - custom line wrapping

        Element element = ElementFactory.createDiv("Hello world"); // <div>Hello world</div>
        element.setText("Hello world");

        element.appendChild(ElementFactory.createSpan()); // <div>Hello world<span></span></div>

        element.setText("Replacement text"); // <div>Replacement text</div>

        element.setText("Welcome back ");

        Element name = ElementFactory.createStrong("Rudolph Reindeer");
        element.appendChild(name); // <div>Welcome back <strong>Rudolph Reindeer</strong></div>

        element.getTextRecursively(); // will return "Welcome back Rudolph Reindeer"
        element.getText(); // will return "Welcome back "

        //@formatter:on
    }
}
