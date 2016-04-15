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
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.ui.Text;

public class BasicComponentView extends AbstractDivView {

    public static final String TEXT = "This is the basic component view text component with some tags: <b><html></body>";
    public static final String BUTTON_TEXT = "Click me";
    public static final String DIV_TEXT = "Hello world";

    @Override
    protected void onShow() {
        getElement().getStyle().set("margin", "1em");
        getElement().setAttribute("id", "root");

        Text text = new Text(TEXT);

        Input input = new Input();
        input.setPlaceholder("Synchronized on change event");

        Button button = new Button(BUTTON_TEXT, e -> {
            Div greeting = new Div();
            greeting.addClassName("thankYou");
            String buttonText = e.getSource().getElement().getOwnTextContent();

            greeting.setText("Thank you for clicking \"" + buttonText
                    + "\" at (" + e.getClientX() + "," + e.getClientY()
                    + ")! The field value is " + input.getValue());

            greeting.addClickListener(e2 -> remove(greeting));
            add(greeting);
        });

        Div helloWorld = new Div();
        helloWorld.setText(DIV_TEXT);
        helloWorld.addClassName("hello");
        helloWorld.setId("hello-world");
        helloWorld.addClickListener(e -> {
            helloWorld.setText("Stop touching me!");
            helloWorld.getElement().getClassList().clear();
        });
        Style s = helloWorld.getElement().getStyle();
        s.set("color", "red");
        s.set("fontWeight", "bold");

        add(text, helloWorld, button, input);
    }

}
