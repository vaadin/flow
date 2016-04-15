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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.UI;

@CodeFor("tutorial-dynamic-content.asciidoc")
public class DynamicContent {

    void tutorialCode() {
        Element name = ElementFactory.createInput("text");
        name.synchronizeProperty("value", "change");
        Element button = ElementFactory.createButton("Generate Image");

        Element image = new Element("object");
        image.setAttribute("type", "image/svg+xml");
        image.getStyle().set("display", "block");

        //@formatter:off - custom line wrapping
        button.addEventListener("click", event -> image.setAttribute("data", createResource(name)));
        //@formatter:on

        UI.getCurrent().getElement().appendChild(name, image, button);
    }

    private StreamResource createResource(Element name) {
        //@formatter:off - custom line wrapping
        StreamResource resource = new StreamResource("image", () -> getImageInputStream(name));
        //@formatter:on
        resource.setContentType("image/svg+xml");
        return resource;
    }

    private InputStream getImageInputStream(Element name) {
        String value = name.getProperty("value");
        if (value == null) {
            value = "";
        }
        String svg = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>"
                + "<svg  xmlns='http://www.w3.org/2000/svg' "
                + "xmlns:xlink='http://www.w3.org/1999/xlink'>"
                + "<rect x='10' y='10' height='100' width='100' "
                + "style=' fill: #90C3D4'/><text x='30' y='30' fill='red'>"
                + value + "</text>" + "</svg>";
        return new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
    }
}
