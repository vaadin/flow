/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementInnerHtmlView", layout = ViewTestLayout.class)
public class ElementInnerHtmlView extends AbstractDivView {

    Div innerHtml;

    @Override
    protected void onShow() {
        innerHtml = new Div();
        innerHtml.setId("inner-html-field");
        add(createButton("Foo"), createButton("Boo"), getNullButton(),
                createToggleButton(), innerHtml);

    }

    private NativeButton createButton(String value) {
        NativeButton button = new NativeButton("Set value " + value,
                click -> innerHtml.getElement().setProperty("innerHTML",
                        String.format("<p>%s</p>", value)));
        button.setId("set-" + value.toLowerCase());
        return button;
    }

    private NativeButton getNullButton() {
        NativeButton button = new NativeButton("Set value null",
                click -> innerHtml.getElement().setProperty("innerHTML", null));
        button.setId("set-null");
        return button;
    }

    private NativeButton createToggleButton() {
        NativeButton button = new NativeButton("ToggleVisibility",
                click -> innerHtml.setVisible(!innerHtml.isVisible()));
        button.setId("toggle-visibility");
        return button;
    }

}
