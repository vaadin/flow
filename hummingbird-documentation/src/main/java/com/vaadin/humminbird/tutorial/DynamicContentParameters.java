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
package com.vaadin.humminbird.tutorial;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.UI;

@CodeFor("tutorial-dynamic-content.asciidoc")
public class DynamicContentParameters {

    void tutorialCode() {
        Element name = ElementFactory.createInput("text");
        name.synchronizeProperty("value", "change");
        Element button = ElementFactory.createButton("Generate Image");

        Element image = new Element("object");
        image.setAttribute("type", "image/svg+xml");
        image.getStyle().set("display", "block");

        //@formatter:off - custom line wrapping
        button.addEventListener("click", event -> image.setAttribute("data", "image?name="+name.getProperty("value")));
        //@formatter:on

        UI.getCurrent().getElement().appendChild(name, image, button);
    }

}
