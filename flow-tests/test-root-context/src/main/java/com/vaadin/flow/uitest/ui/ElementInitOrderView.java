/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.util.stream.Stream;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementInitOrderView", layout = ViewTestLayout.class)
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/ElementInitOrder.html")
@JsModule("ElementInitOrder.js")
public class ElementInitOrderView extends AbstractDivView {
    public ElementInitOrderView() {
        NativeButton reattach = createButton("Reattach components", "reattach",
                event -> reattachElements());

        add(reattach, new Html("<br />"));

        reattachElements();
    }

    private void reattachElements() {
        Stream.of("init-order-polymer", "init-order-nopolymer")
                // Remove old child if present
                .peek(name -> getElement().getChildren()
                        .filter(child -> child.getTag().equals(name))
                        .findFirst().ifPresent(Element::removeFromParent))
                // Create and attach new child
                .map(ElementInitOrderView::createElement)
                .forEach(getElement()::appendChild);
    }

    private static Element createElement(String tag) {
        Element element = new Element(tag);
        element.appendChild(new Element("span"));
        element.getStyle().set("animationName", "style");
        element.getClassList().add("class");
        element.setAttribute("attribute", "attribute");
        element.setProperty("property", "property");
        return element;
    }
}
