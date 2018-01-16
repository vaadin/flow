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
package com.vaadin.flow.tutorial.advanced;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("advanced/tutorial-dynamic-content.asciidoc")
public class DynamicContent {

    void tutorialCode() {
        Input name = new Input();

        Element image = new Element("object");
        image.setAttribute("type", "image/svg+xml");
        image.getStyle().set("display", "block");

        NativeButton button = new NativeButton("Generate Image");
        //@formatter:off - custom line wrapping
        button.addClickListener(event -> {
            StreamResource resource = new StreamResource("image.svg",
                    () -> getImageInputStream(name));
            image.setAttribute("data", resource);
        });
        //@formatter:on

        UI.getCurrent().getElement().appendChild(name.getElement(), image,
                button.getElement());
    }

    private InputStream getImageInputStream(Input name) {
        String value = name.getValue();
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
