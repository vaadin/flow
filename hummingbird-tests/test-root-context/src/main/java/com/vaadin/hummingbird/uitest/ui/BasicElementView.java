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

import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.uitest.component.Button;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.hummingbird.uitest.component.Input;

public class BasicElementView extends AbstractDivView {

    @Override
    protected void onShow() {
        getElement().getStyle().set("margin", "1em");

        Button button = new Button("Click me");

        Input input = new Input()
                .setPlaceholder("Synchronized on change event");

        button.getElement().addEventListener("click", e -> {
            Div greeting = new Div().addClass("thankYou");
            String buttonText = e.getEventData()
                    .getString("element.textContent");
            greeting.setText("Thank you for clicking at \"" + buttonText
                    + "\"! The field value is " + input.getValue());

            greeting.getElement().addEventListener("click",
                    e2 -> removeComponent(greeting));
            addComponent(greeting);
        } , "element.textContent");

        Div helloWorldElement = new Div();
        helloWorldElement.setText("Hello world").addClass("hello")
                .setId("hello-world");
        helloWorldElement.getElement().addEventListener("click", e -> {
            helloWorldElement.getElement().setTextContent("Stop touching me!");
            helloWorldElement.getElement().getClassList().clear();
        });
        Style s = helloWorldElement.getElement().getStyle();
        s.set("color", "red");
        s.set("fontWeight", "bold");

        addComponent(helloWorldElement, button, input);
    }

}
