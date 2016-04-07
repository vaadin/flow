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

import java.util.function.Consumer;

import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.uitest.component.AbstractHtmlComponent;
import com.vaadin.hummingbird.uitest.component.Button;
import com.vaadin.hummingbird.uitest.component.Div;
import com.vaadin.hummingbird.uitest.component.Input;
import com.vaadin.ui.Text;

public class BasicComponentView extends AbstractDivView {

    public static final String TEXT = "This is the basic component view text component with some tags: <b><html></body>";
    public static final String BUTTON_TEXT = "Click me";
    public static final String DIV_TEXT = "Hello world";
    private EventRegistrationHandle remover;

    @Override
    protected void onShow() {
        getElement().getStyle().set("margin", "1em");
        getElement().setAttribute("id", "root");

        Text text = new Text(TEXT);

        Button button = new Button(BUTTON_TEXT);

        Input input = new Input();
        input.setPlaceholder("Synchronized on change event");

        remover = button.addClickListener(
                new Consumer<AbstractHtmlComponent.ClickEvent>() {
                    @Override
                    public void accept(ClickEvent e) {
                        Div greeting = new Div();
                        greeting.addClass("thankYou");
                        String buttonText = "(" + e.getClientX() + ","
                                + e.getClientY() + ")";
                        greeting.setText("Thank you for clicking at \""
                                + buttonText + "\"! The field value is "
                                + input.getValue());

                        greeting.getElement().addEventListener("click",
                                e2 -> removeComponents(greeting));
                        addComponents(greeting);
                        remover.remove();
                    }

                });

        Div helloWorld = new Div(DIV_TEXT);
        helloWorld.addClass("hello");
        helloWorld.setId("hello-world");
        helloWorld.addClickListener(e -> {
            helloWorld.getElement().setTextContent("Stop touching me!");
            helloWorld.getElement().getClassList().clear();
        });
        Style s = helloWorld.getElement().getStyle();
        s.set("color", "red");
        s.set("fontWeight", "bold");

        addComponents(text, helloWorld, button, input);
    }

}
