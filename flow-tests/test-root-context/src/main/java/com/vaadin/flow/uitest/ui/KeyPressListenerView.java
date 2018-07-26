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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.KeyPressListenerView", layout = ViewTestLayout.class)
public class KeyPressListenerView extends Div {

    private static class MyInput extends Input implements KeyNotifier {

    }

    public KeyPressListenerView() {
        MyInput field = new MyInput();
        field.setId("text-field");
        Div div = new Div();
        div.setId("key-event");
        field.addKeyPressListener(Key.ENTER, event -> {
            div.setText("enter");
            ;
        });
        field.addKeyPressListener(Key.ARROW_DOWN, event -> {
            System.out.println("arrow-down");
        });
        add(field, div);
    }
}
